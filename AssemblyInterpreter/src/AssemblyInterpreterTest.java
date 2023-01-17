import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssemblyInterpreterTest {

    AssemblyInterpreter asmi = new AssemblyInterpreter();

    @Test
    void should_move_100_to_eax() {
        asmi.reset();
        asmi.memory[0] = "movl $100, %eax";
        asmi.run();
        assertEquals(100, asmi.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_ebx_to_eax() {
        asmi.reset();
        asmi.memory[0] = "movl $100, %ebx";
        asmi.memory[1] = "movl %ebx, %eax";
        asmi.run();
        assertEquals(100, asmi.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_5_address_to_eax() {
        asmi.reset();
        asmi.memory[5] = "100"; // load data
        asmi.memory[0] = "movl 5, %eax";
        asmi.run();
        assertEquals(100, asmi.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_ebx_pointer_to_eax() {
        asmi.reset();
        asmi.memory[5] = "100"; // load data
        asmi.memory[0] = "movl $5, %ebx";
        asmi.memory[1] = "movl (%ebx), %eax";
        asmi.run();
        assertEquals(100, asmi.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_5_indexed_address_to_eax() {
        asmi.reset();
        asmi.memory[5] = "100"; // load data
        asmi.memory[0] = "movl $1, %ebx";
        asmi.memory[1] = "movl 4(,%ebx,), %eax";
        asmi.run();
        assertEquals(100, asmi.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_ebx_indexed_address_to_eax() {
        asmi.reset();
        asmi.memory[5] = "100"; // load data
        asmi.memory[0] = "movl $4, %ebx";
        asmi.memory[1] = "movl 1(%ebx), %eax";
        asmi.run();
        assertEquals(100, asmi.cpuRegisters.get("%eax"));
    }

}