package com.realtimedatastreaming.realtimedatastreaming.Repository;


import com.realtimedatastreaming.realtimedatastreaming.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findTop10ByOrderByIdDesc();
}
