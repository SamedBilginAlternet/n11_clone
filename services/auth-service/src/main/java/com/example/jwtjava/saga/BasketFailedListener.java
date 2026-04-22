package com.example.jwtjava.saga;

import com.example.jwtjava.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Compensation handler. When basket-service can't provision a basket for a
 * freshly-registered user, we roll back the local side of the saga by deleting
 * the orphan user. The user_roles and refresh_tokens tables cascade on delete,
 * so removing the user is sufficient.
 *
 * This is the "C" in Saga — a forward local commit was made (user saved), and
 * the compensation undoes it because a downstream step couldn't complete.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasketFailedListener {

    private final UserRepository userRepository;

    @Transactional
    @RabbitListener(queues = SagaTopology.BASKET_FAILED_QUEUE)
    public void onBasketCreationFailed(BasketCreationFailedEvent event) {
        log.warn("Received BasketCreationFailed userId={} reason={} — compensating by deleting user",
                event.userId(), event.reason());

        userRepository.findById(event.userId()).ifPresentOrElse(
                user -> {
                    userRepository.delete(user);
                    log.info("Compensation complete: user id={} email={} deleted",
                            event.userId(), event.email());
                },
                () -> log.warn("Compensation skipped: user id={} already gone", event.userId())
        );
    }
}
