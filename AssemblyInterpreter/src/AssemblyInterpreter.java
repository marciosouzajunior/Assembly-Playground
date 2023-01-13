import sun.security.ssl.Debug;

import java.util.HashMap;
import java.util.regex.Pattern;

public class AssemblyInterpreter {

    // Memory to store programs and data (there is no difference between then, except how it is used).
    String[] memory = new String[100];

    // CPU registers:
    // Instruction pointer holds the memory address of the next instruction to be executed.
    int eip = 0;

    // General registers
    HashMap<String, String> cpu = new HashMap<String, String>() {{
        put("%eax", "0");
        put("%ebx", "0");
        put("%ecx", "0");
    }};

    // Patterns used to identify addressing modes
    static Pattern IMMEDIATE_PATTERN = Pattern.compile("^\\$[a-zA-Z0-9_]+$"); // $12
    static Pattern REGISTER_PATTERN = Pattern.compile("^%[a-z]{3}$"); // %eax
    static Pattern DIRECT_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$"); // ADDRESS
    static Pattern INDEXED_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\((%[a-z]{3})*,(%[a-z]{3})*,[124]*\\)$"); // string_start(,%ecx,1)
    static Pattern INDIRECT_PATTERN = Pattern.compile("^\\(%[a-z]{3}\\)$"); // (%eax)
    static Pattern BASE_PATTERN = Pattern.compile("^[0-9]+\\(%[a-z]{3}\\)$"); // 4(%eax)

    public static void main(String[] args) {

        AssemblyInterpreter asmi = new AssemblyInterpreter();

        // Just testing move instruction
        //asmi.memory[10] = "movl 100(,%ecx,1), %eax";
        //asmi.memory[10] = "movl $1, %eax";
        //asmi.memory[10] = "movl 4(%eax), %ebx";
        //asmi.memory[12] = "movl $2, %ebx";

        // Load some values in memory
        asmi.memory[90] = "1";
        asmi.memory[91] = "2";
        asmi.memory[92] = "3";
        asmi.memory[93] = "4";
        asmi.memory[94] = "5";

        // put value at location 90 in ebx to show once program exits
        asmi.memory[10] = "movl 90, %ebx";
        asmi.memory[11] = "movl $1, %eax";
        asmi.memory[12] = "int $0x80";

        asmi.eip = 10;
        asmi.run();

    }

    private void run() {

        do {

            String instruction = memory[eip];
            String[] instructionParts = instruction.split(" ");

            switch (instructionParts[0]) {
                case "movl":
                    movl(instructionParts);
                    break;
                case "int":
                    interrupt(instructionParts);
                    break;
            }

            eip++;

        } while (true);

    }

    private void interrupt(String[] instructionParts) {

        // The int stands for interrupt. The 0x80 is the interrupt number to use.
        // An interrupt interrupts the normal program flow, and transfers control from our program
        // to Linux so that it will do a system call. You can think of it as like signaling Batman.

        String num = instructionParts[1];
        if (num.equals("$0x80")) {

            // Linux knows which system call we want to access by what we stored in the
            // %eax register. Each system call has other requirements as to what needs to
            // be stored in the other registers.

            switch (cpu.get("%eax")) {

                // System call number 1 is the exit system call,
                // which requires the status code to be placed in %ebx.
                case "1":
                    System.out.println("exit status: " + cpu.get("%ebx"));
                    System.exit(0);
                    break;
            }

        }

    }

    private void movl(String[] instructionParts) {

        String source = instructionParts[1];
        String destination = instructionParts[2];

        source = handleSourceAddressing(source);
        handleDestinationAddressing(source, destination);

    }

    private String handleSourceAddressing(String source) {

        source = source.substring(0, source.length() - 1); // Removes last comma

        // Immediate mode
        // The data to access is embedded in the instruction itself.
        if (IMMEDIATE_PATTERN.matcher(source).matches()) {
            return source.substring(1); // Removes the $
        }

        // Register addressing mode
        // The instruction contains a register to access, rather than a memory location.
        if (REGISTER_PATTERN.matcher(source).matches()) {
            return cpu.get(source);
        }

        // Direct addressing mode
        // The instruction contains the memory address to access.
        if (DIRECT_PATTERN.matcher(source).matches()) {
            return memory[Integer.parseInt(source)];
        }

        // Indexed addressing mode
        // The instruction contains a memory address to access,
        // and also specifies an index register to offset that address.
        if (INDEXED_PATTERN.matcher(source).matches()) {
            int finalAddress = calculateIndexedAddressing(source);
            return memory[finalAddress];
        }

        // Indirect addressing mode
        // The instruction contains a register that contains a
        // pointer to where the data should be accessed.
        if (INDIRECT_PATTERN.matcher(source).matches()) {
            source = source.substring(1, 5); // Removes ()
            source = cpu.get(source);
            return memory[Integer.parseInt(source)];
        }

        // Base pointer addressing mode
        // Similar to indirect addressing, but also include a number called the
        // offset to add to the register’s value before using it for lookup.
        if (BASE_PATTERN.matcher(source).matches()) {
            String offset = source.substring(0, source.indexOf("("));
            source = source.substring(source.indexOf("(") + 1, source.length() - 1);
            source = cpu.get(source);
            return memory[Integer.parseInt(source) + Integer.parseInt(offset)];
        }

        return null;

    }

    private void handleDestinationAddressing(String source, String destination) {

        // Register addressing mode
        // The instruction contains a register to access, rather than a memory location.
        if (REGISTER_PATTERN.matcher(destination).matches()) {
            cpu.replace(destination, source);
            return;
        }

        // Direct addressing mode
        // The instruction contains the memory address to access.
        if (DIRECT_PATTERN.matcher(destination).matches()) {
            memory[Integer.parseInt(destination)] = source;
            return;
        }

        // Indexed addressing mode
        // The instruction contains a memory address to access,
        // and also specifies an index register to offset that address.
        if (INDEXED_PATTERN.matcher(destination).matches()) {
            int finalAddress = calculateIndexedAddressing(destination);
            memory[finalAddress] = source;
        }

        // Indirect addressing mode
        // The instruction contains a register that contains a
        // pointer to where the data should be accessed.
        if (INDIRECT_PATTERN.matcher(destination).matches()) {
            destination = destination.substring(1, 5); // Removes ()
            destination = cpu.get(destination);
            memory[Integer.parseInt(destination)] = source;
        }

        // Base pointer addressing mode
        // Similar to indirect addressing, but also include a number called the
        // offset to add to the register’s value before using it for lookup.
        if (BASE_PATTERN.matcher(destination).matches()) {
            String offset = destination.substring(0, destination.indexOf("("));
            destination = destination.substring(destination.indexOf("(") + 1, destination.length() - 1);
            destination = cpu.get(destination);
            memory[Integer.parseInt(destination) + Integer.parseInt(offset)] = source;
        }

    }

    private int calculateIndexedAddressing(String operand) {

        /*
        The general form of memory address references is this:
        ADDRESS_OR_OFFSET(%BASE_OR_OFFSET,%INDEX,MULTIPLIER)
        All of the fields are optional. To calculate the address, simply perform the following calculation:
        FINAL ADDRESS = ADDRESS_OR_OFFSET + %BASE_OR_OFFSET + MULTIPLIER * %INDEX
        */

        String ADDRESS_OR_OFFSET = operand.substring(0, operand.indexOf("("));
        String FIELDS = operand.substring(operand.indexOf("(") + 1, operand.indexOf(")"));
        String[] FIELDS_PARTS = FIELDS.split(",");

        String BASE_OR_OFFSET = FIELDS_PARTS[0];
        if (BASE_OR_OFFSET.equals("")) {
            BASE_OR_OFFSET = "0";
        } else {
            BASE_OR_OFFSET = cpu.get(BASE_OR_OFFSET);
        }

        String INDEX = FIELDS_PARTS[1];
        if (INDEX.equals("")) {
            INDEX = "0";
        } else {
            INDEX = cpu.get(INDEX);
        }

        String MULTIPLIER = FIELDS_PARTS[2];
        if (MULTIPLIER.equals("")) {
            MULTIPLIER = "1";
        }

        return Integer.parseInt(ADDRESS_OR_OFFSET)
                + Integer.parseInt(BASE_OR_OFFSET)
                + Integer.parseInt(MULTIPLIER)
                * Integer.parseInt(INDEX);
    }

}
