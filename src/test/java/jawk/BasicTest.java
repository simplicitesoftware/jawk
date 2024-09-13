package jawk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class BasicTest {
	@Test
	void test() {
		try {
			assertEquals(0, org.jawk.Awk.invoke(new String[] { "-F", ",", "-f", "src/test/resources/test.awk", "src/test/resources/test.txt" }));
		} catch (Exception e) {
			fail(e);
		}
	}
}
