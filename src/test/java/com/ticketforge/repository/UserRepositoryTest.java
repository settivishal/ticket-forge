package com.ticketforge.repository;

import com.ticketforge.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("dev")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and find user by email")
    void testSaveAndFindByEmail() {
        User user = User.builder()
                .id("usr_auth_01")
                .email("alex@example.com")
                .role("ROLE_CUSTOMER")
                .priorityTier(2)
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("alex@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo("usr_auth_01");
        assertThat(found.get().getPriorityTier()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void testUniqueEmailConstraint() {
        userRepository.saveAndFlush(User.builder().id("u1").email("duplicate@test.com").build());

        assertThatThrownBy(() -> userRepository.saveAndFlush(User.builder().id("u2").email("duplicate@test.com").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
