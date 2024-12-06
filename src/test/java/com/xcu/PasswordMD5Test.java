package com.xcu;

import com.xcu.util.PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = EasyPanApplication.class)
@Slf4j
public class PasswordMD5Test {

    // 5g1sojxdn59ph8hj9rnj@74bf7cf92ee40207142647b144e8f426
    @Test
    public void testSaltPasswordMD5() {
        String rawPassword = "liuning19881117";
        String encodedPassword = "5g1sojxdn59ph8hj9rnj@74bf7cf92ee40207142647b144e8f426";
        // log.info("Encoded password: {}", encodedPassword);

        Boolean matches = PasswordEncoder.matches(encodedPassword, rawPassword);
        log.info("Matches: {}", matches);
    }


}
