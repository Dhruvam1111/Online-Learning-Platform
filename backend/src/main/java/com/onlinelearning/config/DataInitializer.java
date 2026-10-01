package com.onlinelearning.config;

import com.onlinelearning.model.User;
import com.onlinelearning.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            logger.info("Database is empty. Seeding default accounts (password: password123)...");

            String passwordHash = passwordEncoder.encode("password123");

            userRepository.save(new User("Dr. Sarah Chen", "sarah@instructor.com", passwordHash, "instructor"));
            userRepository.save(new User("Prof. James Wilson", "james@instructor.com", passwordHash, "instructor"));
            userRepository.save(new User("Alice Johnson", "alice@student.com", passwordHash, "student"));
            userRepository.save(new User("Bob Martinez", "bob@student.com", passwordHash, "student"));
            userRepository.save(new User("Charlie Park", "charlie@student.com", passwordHash, "student"));

            logger.info("Successfully seeded 5 initial test users.");
        }
    }
}
