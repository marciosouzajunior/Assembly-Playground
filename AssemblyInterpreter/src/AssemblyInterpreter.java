import java.util.HashMap;
import java.util.regex.Pattern;

public class AssemblyInterpreter {

    // Memory to store programs and data (there is no difference between then, except how it is used).
    static int MEMORY_SIZE = 100;
    String[] memory = new String[MEMORY_SIZE];

    // CPU registers
    HashMap<String, String> cpuRegisters = new HashMap<String, String>() {{
        put("%eax", "0");
        put("%ebx", "0");
        put("%ecx", "0");
        put("%eip", "0"); // Instruction pointer holds the memory address of the next instruction to be executed.
        put("%esp", String.valueOf(MEMORY_SIZE)); // The stack register, %esp, always contains a pointer to the current top of the stack, wherever it is.
        put("zero_flag", ""); // This flag is set to true if the result of the instruction is zero.
        put("sign_flag", ""); // This is set to the sign of the last result.
    }};

    // Patterns used to identify addressing modes
    static Pattern IMMEDIATE_PATTERN = Pattern.compile("^\\$[a-zA-Z0-9_]+$"); // $12
    static Pattern REGISTER_PATTERN = Pattern.compile("^%[a-z]{3}$"); // %eax
    static Pattern DIRECT_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$"); // ADDRESS
    static Pattern INDEXED_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\((%[a-z]{3})*,(%[a-z]{3})*,[124]*\\)$"); // base_address(offset_address, index, size)
    static Pattern INDIRECT_PATTERN = Pattern.compile("^\\(%[a-z]{3}\\)$"); // (%eax)
    static Pattern BASE_PATTERN = Pattern.compile("^[0-9]+\\(%[a-z]{3}\\)$"); // 4(%eax)

    public static void main(String[] args) {

        AssemblyInterpreter asmi = new AssemblyInterpreter();

        // Just testing move instruction
        //asmi.memory[0] = "movl 100(,%ecx,1), %eax";
        //asmi.memory[0] = "movl $1, %eax";
        //asmi.memory[0] = "movl 4(%eax), %ebx";
        //asmi.memory[0] = "movl $2, %ebx";

        // Load some values in memory
        asmi.memory[49] = "label_test:";
        asmi.memory[50] = "1";
        asmi.memory[51] = "2";
        asmi.memory[52] = "3";
        asmi.memory[53] = "4";
        asmi.memory[54] = "5";

        // Put value at location 50 in ebx to show once program exits
        asmi.memory[0] = "movl 50, %ebx";
        asmi.memory[1] = "movl $1, %eax";
        asmi.memory[2] = "int $0x80";

        // Push a value onto stack and move to exb to show once program exits
        /*
        asmi.memory[0] = "pushl $99";
        asmi.memory[1] = "pushl $88";
        asmi.memory[2] = "movl (%esp), %ebx";
        asmi.memory[3] = "movl $1, %eax";
        asmi.memory[4] = "int $0x80";
        */

        asmi.run();

    }

    public void run() {

        int eip = 0;

        do {

            String instruction = memory[eip];
            if (instruction == null) {
                break;
            }
            instruction = replaceLabel(instruction);

            String[] instructionParts = instruction.split(" ");
            switch (instructionParts[0]) {
                case "movl":
                    movl(instructionParts);
                    break;
                case "int":
                    interrupt(instructionParts);
                    break;
                case "pushl":
                    pushl(instructionParts);
                    break;
                case "popl":
                    popl(instructionParts);
                    break;
                case "cmpl":
                    cmpl(instructionParts);
                    break;
            }

            eip++;
            cpuRegisters.replace("%eip", String.valueOf(eip));

        } while (true);

    }

    private String replaceLabel(String instruction) {
        String address = "";
        // TODO: Implement the function to replace label to address
        return address;
    }

    public void reset() {
        memory = new String[MEMORY_SIZE];
        for (String key : cpuRegisters.keySet()) {
            cpuRegisters.replace(key, "");
        }
        cpuRegisters.replace("%esp", String.valueOf(MEMORY_SIZE));
    }


    private void cmpl(String[] instructionParts) {

        // Compares two integers. It does this by subtracting the first operand from the second.
        // It discards the results, but sets the flags accordingly. Usually used before a conditional jump.

        String operand1 = instructionParts[1];
        String operand2 = instructionParts[2];

        operand1 = getDataFromAddress(operand1);
        operand2 = getDataFromAddress(operand2);

        int result = Integer.parseInt(operand1) - Integer.parseInt(operand2);

        if (result == 0) {
            cpuRegisters.replace("zero_flag", "1");
            cpuRegisters.replace("sign_flag", "0");
        } else if (result > 0) {
            cpuRegisters.replace("zero_flag", "0");
            cpuRegisters.replace("sign_flag", "0");
        } else if (result < 0) {
            cpuRegisters.replace("zero_flag", "0");
            cpuRegisters.replace("sign_flag", "1");
        }

        // Reference: https://reverseengineering.stackexchange.com/a/20897
        // need to think better on this...


    }

    private void pushl(String[] instructionParts) {

        /*
         The computer’s stack lives at the very top addresses of memory.
         You can push values onto the top of the stack through an instruction called pushl,
         which pushes either a register or memory value onto the top of the stack.
         Well, we say it’s the top, but the "top" of the stack is actually the bottom
         of the stack’s memory. Although this is confusing, the reason for it is that
         when we think of a stack of anything - dishes, papers, etc. - we think of adding
         and removing to the top of it. However, in memory the stack starts at the top of
         memory and grows downward due to architectural considerations.
         */

        String address = instructionParts[1];
        String data = getDataFromAddress(address);
        int esp = Integer.parseInt(cpuRegisters.get("%esp"));
        esp--;
        cpuRegisters.replace("%esp", String.valueOf(esp));
        memory[esp] = data;

    }

    private void popl(String[] instructionParts) {

        /*
        If we want to remove something from the stack, we simply use the popl instruction,
        which adds 4 to %esp and puts the previous top value in whatever register you specified.
        pushl and popl each take one operand - the register to push onto the stack for pushl,
        or receive the data that is popped off the stack for popl.
        */

        String address = instructionParts[1];
        int esp = Integer.parseInt(cpuRegisters.get("%esp"));
        String data = memory[esp];
        moveDataToAddress(data, address);
        esp++;
        cpuRegisters.replace("%esp", String.valueOf(esp));

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

            switch (cpuRegisters.get("%eax")) {

                // System call number 1 is the exit system call,
                // which requires the status code to be placed in %ebx.
                case "1":
                    System.out.println("exit status: " + cpuRegisters.get("%ebx"));
                    System.exit(0);
                    break;
            }

        }

    }

    private void movl(String[] instructionParts) {

        String sourceAddress = instructionParts[1];
        String destinationAddress = instructionParts[2];

        sourceAddress = getDataFromAddress(sourceAddress);
        moveDataToAddress(sourceAddress, destinationAddress);

    }

    private String getDataFromAddress(String address) {

        address = address.replaceAll(",$", ""); // Removes last comma

        // Immediate mode
        // The data to access is embedded in the instruction itself.
        if (IMMEDIATE_PATTERN.matcher(address).matches()) {
            return address.substring(1); // Removes the $
        }

        // Register addressing mode
        // The instruction contains a register to access, rather than a memory location.
        if (REGISTER_PATTERN.matcher(address).matches()) {
            return cpuRegisters.get(address);
        }

        // Direct addressing mode
        // The instruction contains the memory address to access.
        if (DIRECT_PATTERN.matcher(address).matches()) {
            return memory[Integer.parseInt(address)];
        }

        // Indexed addressing mode
        // The instruction contains a memory address to access,
        // and also specifies an index register to offset that address.
        if (INDEXED_PATTERN.matcher(address).matches()) {
            int finalAddress = getIndexedAddress(address);
            return memory[finalAddress];
        }

        // Indirect addressing mode
        // The instruction contains a register that contains a
        // pointer to where the data should be accessed.
        if (INDIRECT_PATTERN.matcher(address).matches()) {
            address = address.substring(1, 5); // Removes ()
            address = cpuRegisters.get(address);
            return memory[Integer.parseInt(address)];
        }

        // Base pointer addressing mode
        // Similar to indirect addressing, but also include a number called the
        // offset to add to the register’s value before using it for lookup.
        if (BASE_PATTERN.matcher(address).matches()) {
            String offset = address.substring(0, address.indexOf("("));
            address = address.substring(address.indexOf("(") + 1, address.length() - 1);
            address = cpuRegisters.get(address);
            return memory[Integer.parseInt(address) + Integer.parseInt(offset)];
        }

        return null;

    }

    private void moveDataToAddress(String data, String address) {

        // Do not have immediate type, because we can't move data into a direct value.

        // Register addressing mode
        // The instruction contains a register to access, rather than a memory location.
        if (REGISTER_PATTERN.matcher(address).matches()) {
            cpuRegisters.replace(address, data);
            return;
        }

        // Direct addressing mode
        // The instruction contains the memory address to access.
        if (DIRECT_PATTERN.matcher(address).matches()) {
            memory[Integer.parseInt(address)] = data;
            return;
        }

        // Indexed addressing mode
        // The instruction contains a memory address to access,
        // and also specifies an index register to offset that address.
        if (INDEXED_PATTERN.matcher(address).matches()) {
            int finalAddress = getIndexedAddress(address);
            memory[finalAddress] = data;
        }

        // Indirect addressing mode
        // The instruction contains a register that contains a
        // pointer to where the data should be accessed.
        if (INDIRECT_PATTERN.matcher(address).matches()) {
            address = address.substring(1, 5); // Removes ()
            address = cpuRegisters.get(address);
            memory[Integer.parseInt(address)] = data;
        }

        // Base pointer addressing mode
        // Similar to indirect addressing, but also include a number called the
        // offset to add to the register’s value before using it for lookup.
        if (BASE_PATTERN.matcher(address).matches()) {
            String offset = address.substring(0, address.indexOf("("));
            address = address.substring(address.indexOf("(") + 1, address.length() - 1);
            address = cpuRegisters.get(address);
            memory[Integer.parseInt(address) + Integer.parseInt(offset)] = data;
        }

    }

    private int getIndexedAddress(String operand) {

        /*
        The memory location is determined by the following:
            - A base address
            - An offset address to add to the base address
            - An index to determine which data element to select
            - The size of the data element
        The format of the expression is: base_address(offset_address, index, size)
        The data value retrieved is located at: base_address + offset_address + index * size
        Base address and size must both be constants, while the other two must be registers.
        If any of the pieces is left out, it is just substituted with zero in the equation.

        References:
        https://gist.github.com/DmitrySoshnikov/c67cbde1cceb0d6a194830b41baa5c8b
        Book Programming from the Ground Up, pages 41 and 42.
        */

        String baseAddress = operand.substring(0, operand.indexOf("("));
        String values = operand.substring(operand.indexOf("(") + 1, operand.indexOf(")"));
        String[] valuesParts = values.split(",", -1);

        String offset = valuesParts[0];
        String index = valuesParts[1];
        String size = valuesParts[2];

        // Handle omitted
        offset = offset.equals("") ? "0" : cpuRegisters.get(offset);
        index = index.equals("") ? "0" : cpuRegisters.get(index);
        size = size.equals("") ? "1" : size;

        return Integer.parseInt(baseAddress)
                + Integer.parseInt(offset)
                + Integer.parseInt(index)
                * Integer.parseInt(size);

    }

}
