import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class AssemblySimulator {

    // Memory to store programs and data (there is no difference between then, except how it is used).
    static int MEMORY_SIZE = 100;
    String[] memory = new String[MEMORY_SIZE];

    // CPU registers
    HashMap<String, Integer> cpuRegisters = new HashMap<String, Integer>() {{
        put("%eax", 0);
        put("%ebx", 0);
        put("%ecx", 0);
        put("%edi", 0);
        put("%eip", 0); // Instruction pointer holds the memory address of the next instruction to be executed.
        put("%esp", MEMORY_SIZE); // The stack register, %esp, always contains a pointer to the current top of the stack, wherever it is.
        put("cmp_flag", 0); // This is a custom flag to store the instruction result.
    }};

    // Patterns used to identify addressing modes
    static Pattern IMMEDIATE_PATTERN = Pattern.compile("^\\$[a-zA-Z0-9_]+$"); // $12
    static Pattern REGISTER_PATTERN = Pattern.compile("^%[a-z]{3}$"); // %eax
    static Pattern DIRECT_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$"); // ADDRESS
    static Pattern INDEXED_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+\\((%[a-z]{3})*,(%[a-z]{3})*,[124]*\\)$"); // base_address(offset_address, index, size)
    static Pattern INDIRECT_PATTERN = Pattern.compile("^\\(%[a-z]{3}\\)$"); // (%eax)
    static Pattern BASE_PATTERN = Pattern.compile("^[0-9]+\\(%[a-z]{3}\\)$"); // 4(%eax)

    public static void main(String[] args) {

        AssemblySimulator as = new AssemblySimulator();

        // Load some values in memory
        as.memory[49] = "numbers:";
        as.memory[50] = "3";
        as.memory[51] = "67";
        as.memory[52] = "34";
        as.memory[53] = "222";
        as.memory[54] = "0";

        // find maximum
        as.memory[0] = "_start:                         ";
        as.memory[1] = "movl $0, %edi                   "; // move 0 to index register
        as.memory[2] = "movl numbers(,%edi,4), %eax     "; // load first byte of data
        as.memory[3] = "movl %eax, %ebx                 "; // first item is the biggest

        as.memory[4] = "start_loop:                     ";
        as.memory[5] = "cmpl $0, %eax                   "; // check if hit the end
        as.memory[6] = "je loop_exit                    ";
        as.memory[7] = "incl %edi                       "; // load next value
        as.memory[8] = "movl numbers(,%edi,1), %eax     ";
        as.memory[9] = "cmpl %ebx, %eax                 "; // compare values
        as.memory[10] = "jle start_loop                 "; // jump to loop beginning if the new is not bigger
        as.memory[11] = "movl %eax, %ebx                "; // move the value as the largest
        as.memory[12] = "jmp start_loop                 ";

        as.memory[13] = "loop_exit:                     ";
        as.memory[14] = "movl $1, %eax                  ";  // 1 is the exit() syscall
        as.memory[15] = "int $0x80                      ";

        as.run();

    }

    public void run() {

        int eip;
        String instruction;
        String[] instructionParts;
        boolean jump;
        replaceLabels();

        do {

            eip = cpuRegisters.get("%eip");
            instruction = memory[eip];
            if (instruction == null) {
                break;
            }
            jump = false;
            instructionParts = instruction.trim().split(" ");
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
                case "jmp":
                    jmp(instructionParts);
                    jump = true;
                    break;
                case "je":
                    jump = je(instructionParts);
                    break;
                case "jle":
                    jump = jle(instructionParts);
                    break;
                case "incl":
                    incl(instructionParts);
                    break;
            }

            // if not jump, increment eip
            if (!jump) {
                cpuRegisters.replace("%eip", ++eip);
            }

        } while (true);

    }

    private void replaceLabels() {

        // Put all labels in a hashmap
        HashMap<String, Integer> labels = new HashMap<>();

        for (int i = 0; i < MEMORY_SIZE; i++) {
            String instruction = memory[i];
            if (instruction == null) {
                continue;
            }
            if (instruction.contains(":")) {
                labels.put(instruction.trim().replace(":", ""), i);
            }
        }

        // Replace all labels
        // TODO: Can we optimize this?
        for (Map.Entry<String, Integer> entry : labels.entrySet()) {
            String label = entry.getKey();
            Integer location = entry.getValue();

            for (int i = 0; i < MEMORY_SIZE; i++) {
                String instruction = memory[i];
                if (instruction == null) {
                    continue;
                }
                if (instruction.contains(label)
                        && i != location) { // do not replace itself
                    location++; // point to next instruction after label
                    memory[i] = instruction.replace(label, location.toString());
                }
            }
        }

    }

    public void reset() {
        memory = new String[MEMORY_SIZE];
        for (String key : cpuRegisters.keySet()) {
            cpuRegisters.replace(key, 0);
        }
        cpuRegisters.replace("%esp", MEMORY_SIZE);
    }

    private void incl(String[] instructionParts) {

        String address = instructionParts[1];
        String value = getDataFromAddress(address);
        int valueInc = Integer.parseInt(value) + 1;
        moveDataToAddress(String.valueOf(valueInc), address);

    }

    private void jmp(String[] instructionParts) {

        // Jump no matter what. This does not need to be preceded by a comparison.
        String location = instructionParts[1];
        cpuRegisters.replace("%eip", Integer.parseInt(location));

    }

    private boolean je(String[] instructionParts) {

        /*
        This is a flow control instruction which says to jump to the location if
        the values that were just compared are equal (that's what the e of je means).
        It uses the status register to hold the value of the last comparison.
        */

        if (cpuRegisters.get("cmp_flag").equals(0)) { // zero means equal
            String location = instructionParts[1];
            cpuRegisters.replace("%eip", Integer.parseInt(location));
            return true;
        }
        return false;

    }

    private boolean jle(String[] instructionParts) {

        // Jump if the second value was less than or equal to the first value
        if (cpuRegisters.get("cmp_flag").equals(0) // zero means equal
                || cpuRegisters.get("cmp_flag").equals(-1)) { // -1 means op2 < op1
            String location = instructionParts[1];
            cpuRegisters.replace("%eip", Integer.parseInt(location));
            return true;
        }
        return false;

    }

    private void cmpl(String[] instructionParts) {

        // Compares two integers. It does this by subtracting the first operand from the second.
        // It discards the results, but sets the flags accordingly. Usually used before a conditional jump.

        String operand1 = instructionParts[1];
        String operand2 = instructionParts[2];

        operand1 = getDataFromAddress(operand1);
        operand2 = getDataFromAddress(operand2);

        int operand1Int = Integer.parseInt(operand1);
        int operand2Int = Integer.parseInt(operand2);

        // In real life, the flags register contains multiple flags (zero flag, carry flag, etc.).
        // To keep things simple, we are using a single flag to store the instruction result.
        if (operand1Int == operand2Int) {
            cpuRegisters.replace("cmp_flag", 0);
        } else if (operand2Int > operand1Int) {
            cpuRegisters.replace("cmp_flag", 1);
        } else if (operand2Int < operand1Int) {
            cpuRegisters.replace("cmp_flag", -1);
        }

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
        int esp = cpuRegisters.get("%esp");
        cpuRegisters.replace("%esp", --esp);
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
        int esp = cpuRegisters.get("%esp");
        String data = memory[esp];
        moveDataToAddress(data, address);
        cpuRegisters.replace("%esp", ++esp);

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
                case 1:
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
            return String.valueOf(cpuRegisters.get(address));
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
            address = String.valueOf(cpuRegisters.get(address));
            return memory[Integer.parseInt(address)];
        }

        // Base pointer addressing mode
        // Similar to indirect addressing, but also include a number called the
        // offset to add to the register’s value before using it for lookup.
        if (BASE_PATTERN.matcher(address).matches()) {
            String offset = address.substring(0, address.indexOf("("));
            address = address.substring(address.indexOf("(") + 1, address.length() - 1);
            address = String.valueOf(cpuRegisters.get(address));
            return memory[Integer.parseInt(address) + Integer.parseInt(offset)];
        }

        return null;

    }

    private void moveDataToAddress(String data, String address) {

        // Do not have immediate type, because we can't move data into a direct value.

        // Register addressing mode
        // The instruction contains a register to access, rather than a memory location.
        if (REGISTER_PATTERN.matcher(address).matches()) {
            cpuRegisters.replace(address, Integer.parseInt(data));
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
            address = String.valueOf(cpuRegisters.get(address));
            memory[Integer.parseInt(address)] = data;
        }

        // Base pointer addressing mode
        // Similar to indirect addressing, but also include a number called the
        // offset to add to the register’s value before using it for lookup.
        if (BASE_PATTERN.matcher(address).matches()) {
            String offset = address.substring(0, address.indexOf("("));
            address = address.substring(address.indexOf("(") + 1, address.length() - 1);
            address = String.valueOf(cpuRegisters.get(address));
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

        int baseAddressInt = Integer.parseInt(baseAddress);
        int offsetInt = offset.equals("") ? 0 : cpuRegisters.get(offset);
        int indexInt = index.equals("") ? 0 : cpuRegisters.get(index);
        int sizeInt = size.equals("") ? 1 : Integer.parseInt(size);

        return baseAddressInt + offsetInt + indexInt * sizeInt;

    }

}
