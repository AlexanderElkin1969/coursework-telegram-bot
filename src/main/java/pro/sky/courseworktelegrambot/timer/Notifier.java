package pro.sky.courseworktelegrambot.timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import pro.sky.courseworktelegrambot.entities.*;
import pro.sky.courseworktelegrambot.repositories.*;
import pro.sky.courseworktelegrambot.services.TelegramBot;
import pro.sky.courseworktelegrambot.services.TelegramBotSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  Проверяет каждый день в 21:01 все ежедневные отчеты усыновителей.<br>
 *  Если усыновитель не прислал, или прислал не полный отчет напоминает ему об этом.
 *  Если усыновитель не присылает отчет более 2 дней извещает волонтера.<br>
 *  Проверяет каждый день в 23:01 все усыновления.<br>
 *  Если пользователю не продлили испытательный период, поздравляет его.
 *  */
@Component
public class Notifier {

    private static final Logger logger = LoggerFactory.getLogger(Notifier.class);

    private final CatAdoptionRepository catAdoptionRepository;
    private final DogAdoptionRepository dogAdoptionRepository;
    private final CatReportRepository catReportRepository;
    private final DogReportRepository dogReportRepository;
    private final MessageToVolunteerRepository messageToVolunteerRepository;
    private final TelegramBotSender telegramBotSender;

    public Notifier(CatAdoptionRepository catAdoptionRepository,
                    DogAdoptionRepository dogAdoptionRepository,
                    CatReportRepository catReportRepository,
                    DogReportRepository dogReportRepository,
                    MessageToVolunteerRepository messageToVolunteerRepository,
                    TelegramBotSender telegramBotSender) {
        this.catAdoptionRepository = catAdoptionRepository;
        this.dogAdoptionRepository = dogAdoptionRepository;
        this.catReportRepository = catReportRepository;
        this.dogReportRepository = dogReportRepository;
        this.messageToVolunteerRepository = messageToVolunteerRepository;
        this.telegramBotSender = telegramBotSender;
    }

    /**
     *  Проверяет каждый день в 21:01 все ежедневные отчеты усыновителей.<br>
     *  Если усыновитель не прислал, или прислал не полный отчет напоминает ему об этом.Используется метод <u>sendNotification</u> этого сервиса.
     *  Полноту данных проверяем методамм {@link  DogReportRepository#findByDateAndPhotoIsNotNullAndTextIsNotNull(LocalDate)} и
     *  {@link CatReportRepository#findByDateAndPhotoIsNotNullAndTextIsNotNull(LocalDate)}
     *  Если усыновитель не присылает отчет более 2 дней извещает волонтера.
     *  посредством {@link MessageToVolunteerRepository#save(Object)} save()}
     * */
    @Scheduled(cron = "0 1 21 * * *")
    private void sendWarningNoReport(){
        long today = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.of(2022, 12,31));
        long dayLatestReport;
        LocalDate date;
        Report report;

        List<DogAdoption> currentDogAdoptionList = dogAdoptionRepository.findByTrialDateGreaterThanEqual(LocalDate.now());
        Map<Adoption, LocalDate> todayDogReports = dogReportRepository.findByDateAndPhotoIsNotNullAndTextIsNotNull(LocalDate.now())
                .stream()
                .collect(Collectors.toMap(DogReport::getAdoption, Report::getDate));
        for (DogAdoption adoption:currentDogAdoptionList) {
            if (todayDogReports.containsKey(adoption)) {
                break;
            }else{
                report = dogReportRepository.findLatestReport(adoption);
                date = (report != null) ? report.getDate() : adoption.getDate();
                dayLatestReport = ChronoUnit.DAYS.between(date, LocalDate.of(2022, 12,31));
                if(today-dayLatestReport > 2){
                    MessageToVolunteer notification = new MessageToVolunteer();
                    notification.setUser(adoption.getUser());
                    notification.setQuestionTime(LocalDateTime.now());
                    notification.setQuestion("ВНИМАНИЕ !!! Данный опекун не присылал ежедневный отчет более 2 дней");
                    messageToVolunteerRepository.save(notification);
                }else {
                    sendNotification(adoption, "ВНИМАНИЕ !!! " +
                            "Просим вас присылать ежедневный отчет до 21:00.");
                }
            }
        }

        List<CatAdoption> currentCatAdoptionList = catAdoptionRepository.findByTrialDateGreaterThanEqual(LocalDate.now());
        Map<Adoption, LocalDate> todayCatReports = catReportRepository.findByDateAndPhotoIsNotNullAndTextIsNotNull(LocalDate.now())
                .stream()
                .collect(Collectors.toMap(CatReport::getAdoption, Report::getDate));
        for (CatAdoption adoption:currentCatAdoptionList) {
            if (todayCatReports.containsKey(adoption)) {
                break;
            }else{
                report = catReportRepository.findLatestReport(adoption);
                date = (report != null) ? report.getDate() : adoption.getDate();
                dayLatestReport = ChronoUnit.DAYS.between(date, LocalDate.of(2022, 12,31));
                if(today-dayLatestReport > 2){
                    MessageToVolunteer notification = new MessageToVolunteer();
                    notification.setUser(adoption.getUser());
                    notification.setQuestionTime(LocalDateTime.now());
                    notification.setQuestion("ВНИМАНИЕ !!! Данный опекун не присылал ежедневный отчет более 2 дней");
                    messageToVolunteerRepository.save(notification);
                }else {
                    sendNotification(adoption, "ВНИМАНИЕ !!! " +
                                    "Просим вас присылать ежедневный отчет до 21:00.");
                }
            }
        }
    }

    /**
     *  Проверяет каждый день в 23:01 все усыновления.<br>
     *  Если пользователю не продлили испытательный период, поздравляет его.
     *  Используется метод <u>sendNotification</u> этого сервиса.
     * */
    @Scheduled(cron = "0 1 23 * * *")
    private void sendCongratulation(){
        List<DogAdoption> currentDogAdoptionList = dogAdoptionRepository.findByTrialDateGreaterThanEqual(LocalDate.now());
        for (DogAdoption adoption : currentDogAdoptionList) {
            if (adoption.getTrialDate().isEqual(LocalDate.now())) {
                sendNotification(adoption, "Поздравляем !!! Вы успешно прошли испытательный период. " +
                        "Всего наилучшего Вам и вашему питомцу.");
            }
        }
        List<CatAdoption> currentCatAdoptionList = catAdoptionRepository.findByTrialDateGreaterThanEqual(LocalDate.now());
        for (CatAdoption adoption : currentCatAdoptionList) {
            if (adoption.getTrialDate().isEqual(LocalDate.now())) {
                sendNotification(adoption, "Поздравляем !!! Вы успешно прошли испытательный период. " +
                        "Всего наилучшего Вам и вашему питомцу.");
            }
        }
    }

    /**
     *  Отправляет уведомление пользователю, проходящему испытательный период.<br>
     *  Используется метод сервиса {@link TelegramBot#sendMessageToUser(User, String, int)}  }
     *  @param adoption (пользователь является усыновителем, получаем через adoption.getUser())
     *  @param notification текст уведомления.
     * */
    public void sendNotification(Adoption adoption, String notification){
        //это почти тоже что sendMessageToUser, но не сигналит об ошибке отправки
        //что не допустимо для вызова из контроллеров.
        //а если по расписанию, то можно ошибку и проигнорировать
        try {
            telegramBotSender.sendMessageToUser(adoption.getUser(), notification, 0);
        }catch (TelegramApiException e){
            logger.error("TelegramError " + e.getMessage());
        }
    }

}
