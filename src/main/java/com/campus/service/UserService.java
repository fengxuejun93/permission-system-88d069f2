package com.campus.service;

import com.campus.model.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private final Map<Long, User> users = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.incrementAndGet());
        }
        users.put(user.getId(), user);
        return user;
    }

    public User findById(Long id) {
        return users.get(id);
    }

    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    public List<User> findByIds(Set<Long> ids) {
        List<User> result = new ArrayList<>();
        for (Long id : ids) {
            User u = users.get(id);
            if (u != null) result.add(u);
        }
        return result;
    }

    public void setOnline(Long userId, boolean online) {
        User u = users.get(userId);
        if (u != null) u.setOnline(online);
    }
}
