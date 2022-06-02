public class AssemblyInterpreter {

    // Memory to store programs and data (there is no difference between then, except how it is used).
    String[] memory = new String[100];

    // CPU registers:
    // Instruction pointer holds the memory address of the next instruction to be executed.
    int eip = 0;

    // General registers
    String eax = "";
    String ebx = "";

    public static void main(String[] args) {

        AssemblyInterpreter asmi = new AssemblyInterpreter();

        asmi.memory[10] = "movl $1, %eax";
        //asmi.memory[11] = "movl (%eax), %ebx"; //"mov $1, %eax";
        //asmi.memory[12] = "movl $2, %ebx";
        asmi.memory[11] = "int 0x80";

        asmi.eip = 10;
        asmi.run();

    }

    private void run() {

        do {

            String instruction = memory[eip];
            String instructionParts[] = instruction.split(" ");

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

        String num = instructionParts[1].replace(",", "");
        if (num.equals("0x80")) {
            System.exit(Integer.valueOf(eax));
        }

    }

    private void movl(String[] instructionParts) {

        String source = instructionParts[1].replace(",", "");
        String destination = instructionParts[2];

        //
        // Handle source addressing
        //
        if (source.startsWith("$")) {

            // Immediate mode, the data to access is embedded in the instruction itself.
            source = source.replace("$", "");

        } else if (source.startsWith("%")) {

            // Register mode simply moves data in or out of a register.
            if (source.equals("%eax")) {
                source = eax;
            } else if (source.equals("%ebx")) {
                source = ebx;
            }

        } else if (source.contains("(")) {

            // Indirect addressing mode, the instruction contains a register
            // that contains a pointer to where the data should be accessed.
            String offset = source.substring(0, source.indexOf("("));
            if (offset.equals("")) {
                offset = "0";
            }
            String address = source.substring(source.indexOf("(") + 1, source.length() - 1);

            if (address.equals("%eax")) {
                source = memory[Integer.valueOf(eax) + Integer.valueOf(offset)];
            } else if (address.equals("%ebx")) {
                source = memory[Integer.valueOf(ebx) + Integer.valueOf(offset)];
            } else {
                source = memory[Integer.valueOf(address) + Integer.valueOf(offset)];
            }

        } else {

            // Direct addressing mode, the instruction contains the memory address to access.
            source = memory[Integer.valueOf(source)];

        }

        //
        // Handle destination addressing
        //
        if (destination.startsWith("%")) {

            // Register mode simply moves data in or out of a register.
            if (destination.equals("%eax")) {
                eax = source;
            } else if (destination.equals("%ebx")) {
                ebx = source;
            }

        } else if (destination.contains("(")) {

            // Indirect addressing mode, the instruction contains a register
            // that contains a pointer to where the data should be accessed.
            String offset = destination.substring(0, destination.indexOf("("));
            if (offset.equals("")) {
                offset = "0";
            }
            String address = destination.substring(destination.indexOf("(") + 1, destination.length() - 1);

            if (destination.equals("%eax")) {
                memory[Integer.valueOf(eax) + Integer.valueOf(offset)] = source;
            } else if (address.equals("%ebx")) {
                memory[Integer.valueOf(ebx) + Integer.valueOf(offset)] = source;
            } else {
                memory[Integer.valueOf(address) + Integer.valueOf(offset)] = source;
            }

        } else {

            // Direct addressing mode, the instruction contains the memory address to access.
            memory[Integer.valueOf(destination)] = source;

        }

    }

}
