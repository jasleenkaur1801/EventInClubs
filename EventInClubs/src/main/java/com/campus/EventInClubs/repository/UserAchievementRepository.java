package com.campus.EventInClubs.repository;

import com.campus.EventInClubs.domain.model.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    
    List<UserAchievement> findByUserIdOrderByEarnedAtDesc(Long userId);
    
    Optional<UserAchievement> findByUserIdAndAchievementType(Long userId, UserAchievement.AchievementType achievementType);
    
    @Query("SELECT SUM(ua.pointsEarned) FROM UserAchievement ua WHERE ua.user.id = :userId")
    Long getTotalPointsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ua FROM UserAchievement ua WHERE ua.user.id = :userId AND ua.achievementType = :type AND ua.level = :level")
    Optional<UserAchievement> findByUserIdAndTypeAndLevel(@Param("userId") Long userId, 
                                                         @Param("type") UserAchievement.AchievementType type,
                                                         @Param("level") Integer level);
    
    @Query("SELECT COUNT(ua) FROM UserAchievement ua WHERE ua.user.id = :userId")
    long countAchievementsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT u, COALESCE(SUM(ua.pointsEarned), 0) as totalPoints, COUNT(ua) as achievementCount " +
           "FROM User u LEFT JOIN UserAchievement ua ON u.id = ua.user.id " +
           "WHERE u.role = 'STUDENT' " +
           "GROUP BY u.id, u.name, u.email " +
           "ORDER BY totalPoints DESC, achievementCount DESC " +
           "LIMIT :limit")
    List<Object[]> getLeaderboardData(@Param("limit") int limit);
}
