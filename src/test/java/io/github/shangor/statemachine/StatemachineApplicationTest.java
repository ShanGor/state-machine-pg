package io.github.shangor.statemachine;

import io.github.shangor.statemachine.util.SecurityUtil;
import org.junit.jupiter.api.Test;

//@SpringBootTest
class StatemachineApplicationTest {

	@Test
	void contextLoads() {
	}

//	@Test
	void testEsapi() {
		SecurityUtil.sanitizeSQL("");
	}
}
