package com.brunoarruda.pgc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

public class TestUseCases {

	/**
	 * if you are opening stefano81-dcpabe as the root project folder on your IDE,
	 * remove the part "sistemas\\stefano81-dcpabe\\" from TEST_PATH
	 * */
	final static String TEST_PATH = "sistemas\\stefano81-dcpabe\\test data";
	final static private PrintStream realSystemOut = System.out;
	final static private PrintStream realSystemErr = System.err;

	public static void main(String[] args) {
		System.out.println("Tests:\n");

		// create root folder to store test files, nothing happens if it already exists
		File root_test_path = new File(TEST_PATH);
		root_test_path.mkdir();
		
		// needed to clean test path directory
		for (File file : root_test_path.listFiles()) {
			if (file.isDirectory()) {
				for (File innerFile : file.listFiles()) {
					innerFile.delete();					
				}
			}
			file.delete();
		}

		runTestMethods();
	}
	private static class NullOutputStream extends OutputStream {
		@Override
		public void write(int b){
			return;
		}
		@Override
		public void write(byte[] b){
			return;
		}
		@Override
		public void write(byte[] b, int off, int len){
			return;
		}
		public NullOutputStream(){
		}
	}

	private static void runTestMethods() {
		Method[] methods = TestUseCases.class.getDeclaredMethods();
		for (Method m : methods) {
			if (m.getName().startsWith("test")) {
				String testDir = TEST_PATH + File.separator + m.getName();
				try {
					System.out.print(String.format("running: %s ... ", m.getName()));

					// disables output streams from code to not mess the test logging
					System.setOut(new PrintStream(new NullOutputStream()));
					System.setErr(new PrintStream(new NullOutputStream()));

					// invoke the function which name starts with 'test'
					m.invoke(null, testDir);

					// restores the output from System.out
					System.setOut(realSystemOut);
					System.setErr(realSystemErr);
					if (m.getName().contains("shouldFail")) {
						System.out.println("FAILED.");
					} else {
						System.out.println("OK.");
					}
				} catch (Exception e) {
					// restores the output from System.out
					System.setOut(realSystemOut);
					System.setErr(realSystemErr);
					String result = "FAILED.";
					if(m.getName().contains("shouldFail")) {
						result = "OK.";
					}
					String fileDir = testDir + File.separator + "log_error.txt";
					try (PrintWriter pw = new PrintWriter(new FileWriter(fileDir, true))) {
						e.printStackTrace(pw);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					System.out.println(result + " (check the log on the test folder)");
				}
			}
		}
	}

	public static void testDecrypt_WithOneAttribute(String testDir) {
		String[] names = { "Bob" };
		String[] attributes = { "paciente" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "paciente");
		test.decrypt("", names[0], test.searchKeys(names[0]));
		test.check(testDir, names[0], "paciente", test.searchKeys(names[0]));
	}

	public static void testDecrypt_WithTwoAttributes(String testDir) {
		String[] names = { "Bob", "Bob" };
		String[] attributes = {"paciente", "dono-do-prontuário"};
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "and dono-do-prontuário paciente");
		test.decrypt("", names[0], test.searchKeys(names[0]));
	}

	public static void testPolicy_AndGateWithMissingAttribute_shouldFail(String testDir) {
		String[] names = { "Bob", "Bob", "Alice" };
		String[] attributes = { "paciente", "dono-do-prontuário", "usuário-credenciado" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "and usuário-credenciado and dono-do-prontuário paciente");
		test.decrypt("", names[0], test.searchKeys(names[0]));

	}

	public static void testPolicy_OrGateWithMissingAttribute(String testDir) {
		String[] names = { "Bob", "Alice" };
		String[] attributes = { "dono-do-prontuário", "médico" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "or dono-do-prontuário médico");
		test.decrypt("", names[0], test.searchKeys(names[0]));
	}

	public static void testDecrypt_WithKeyFromOtherUser_shouldFail(String testDir) {
		String[] names = { "Bob", "Alice" };
		String[] attributes = { "dono-do-prontuário", "hacker" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "dono-do-prontuário");
		test.decrypt("", "Alice", test.searchKeys("Bob"));
		test.check(testDir, "Alice", "dono-do-prontuário", test.searchKeys("Bob"));
	}

	public static void testDecrypt_UserWithOwnAndStolenKeys_shouldFail(String testDir) {
		String[] names = { "Bob", "Alice" };
		String[] attributes = { "paciente", "dono-do-prontuário" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "and dono-do-prontuário paciente");
		String[] keyPath = { test.searchKeys("Bob")[0], test.searchKeys("Alice")[0] };
		test.decrypt("", "Bob", keyPath);
		test.check(testDir, "Bob", "and dono-do-prontuário paciente", keyPath);
	}

	public static void testEncrypt_WithLargeFile(String testDir) {
		String[] names = { "Bob" };
		String[] attributes = { "paciente" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);

		String fileDir = testDir + File.separator + "BigFile.txt";		
		try (			
			FileWriter fr = new FileWriter(new File(fileDir));
            BufferedWriter bw = new BufferedWriter(fr);
		){
			
			for (int i = 0; i < 100000; i++) {
				bw.write("This is mock for a Big File sent to encryption with an ABE scheme.\n");
			}			
		} catch (Exception e) {
			e.printStackTrace();
		}
		test.encrypt(fileDir, "paciente");
		String[] keyPath = test.searchKeys(names[0]);
		test.decrypt(fileDir, names[0], keyPath);
	}

	public static void testKeyGen_GenerateKeyWithEmptyName(String testDir) {
		String[] names = {""};
		String[] attributes = {"paciente"};
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "paciente");
		String[] keyPath = test.searchKeys(names[0]);
		test.decrypt("", names[0], keyPath);
	}

	public static void testKeyGen_MultipleAttributes(String testDir) {
		String[] names = { "" };
		String[] attributes = { "paciente" };
		String authorityName = "Hospital";

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", "paciente");
		String[] keyPath = test.searchKeys(names[0]);
		test.decrypt("", names[0], keyPath);
	}

	// policySize must be a power of 2 in order to make this algorithm work.
	private static String generatePolicy(int policySize, String[] operators, String[] attributes) {
		Stack<String> stack = new Stack<String>();
		for (int i = 0; i < attributes.length; i++) {
			stack.push(attributes[i]);
			int elements = (i + 1);

			while ( elements % 2 == 0) {
				String rightPolicy = stack.pop();
				String leftPolicy = stack.pop();
				int decisor = new Random().nextInt(operators.length);
				String aggregation = String.format("%s %s %s", operators[decisor], leftPolicy, rightPolicy);
				// stack.add((int) log2i - 1, aggregation);
				stack.push(aggregation);
				elements = elements / 2;
			}
		}
		return stack.toString().replace("[", "").replace("]", "");
	}

	public static void testPolicy_HundredsOfOrOperatorsWorks(String testDir) {
		/*
		 * the policy generation algorithm derives a complete binary tree of or, thus
		 * policySize must be a power of 2
		*/
		int policySize = 64;
		String[] names = new String[policySize];
		String[] operators = {"or"};
		String authorityName = "Hospital";

		List<String> temp = new ArrayList<String>();
		for (int i = 0; i < names.length; i++) {
			names[i] = "Alice";
		}
		for (int i = 0; i < names.length; i++) {
			temp.add(String.format("attribute-%03d", i));
		}

		String[] attributes = temp.toArray(new String[temp.size()]);
		String bigPolicy = generatePolicy(policySize, operators, attributes);

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", bigPolicy);
		test.decrypt("", names[0], test.searchKeys(names[0]));
	}

	public static void testPolicy_HundredsOfAndOperatorsWorks(String testDir) {
		/*
		 * the policy generation algorithm derives a complete binary tree of or, thus
		 * policySize must be a power of 2
		*/
		int policySize = 64;
		String[] names = new String[policySize];
		String[] operators = {"and"};
		String authorityName = "Hospital";

		List<String> temp = new ArrayList<String>();
		for (int i = 0; i < names.length; i++) {
			names[i] = "Alice";
		}
		for (int i = 0; i < names.length; i++) {
			temp.add(String.format("attribute-%03d", i));
		}

		String[] attributes = temp.toArray(new String[temp.size()]);
		String bigPolicy = generatePolicy(policySize, operators, attributes);

		UseCase test = new UseCase(authorityName, testDir, attributes);
		test.globalSetup();
		test.authoritySetup();
		test.keyGeneration(names, attributes);
		test.encrypt("", bigPolicy);
		test.decrypt("", names[0], test.searchKeys(names[0]));

	}
}
