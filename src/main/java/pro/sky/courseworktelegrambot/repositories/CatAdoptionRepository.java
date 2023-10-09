package pro.sky.courseworktelegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pro.sky.courseworktelegrambot.entities.*;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CatAdoptionRepository extends JpaRepository<CatAdoption, Integer> {
    List<CatAdoption> findByUserAndDateLessThanEqualAndTrialDateGreaterThanEqual(
            //trialDate_parameter >= date_base AND date_parameter <= trialDate_base
            User user, LocalDate trialDate, LocalDate Date);
    //поиск усыновлений животного с пересекающимся испытательным сроком,
    List<CatAdoption> findByPetAndDateLessThanEqualAndTrialDateGreaterThanEqual(
            //trialDate_parameter >= date_base AND date_parameter <= trialDate_base
            Pet pet, LocalDate trialDate, LocalDate Date);
    //поиск активных усыновлений, действующих на дату с проверкой обех границ
    //от даты усыновления до конца испытательного срока (использовал Павел)
    List<CatAdoption> findByDateLessThanEqualAndTrialDateGreaterThanEqual(
            //date_parameter >= date_base AND date_parameter <= trialDate_base
            LocalDate date1, LocalDate date2);
    //поиск активных усыновлений, действующих на дату с проверкой только верхней границы
    //(использовал Александр)
    List<CatAdoption> findByTrialDateGreaterThanEqual(LocalDate date);
    //Поиск усыновлений, в которых на дату-параметр заканчивается испытательный срок.
    //Для поздравлений
    List<CatAdoption> findByTrialDate(LocalDate date);
}
