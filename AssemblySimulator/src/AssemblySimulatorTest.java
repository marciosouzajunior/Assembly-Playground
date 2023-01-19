import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AssemblySimulatorTest {

    AssemblySimulator as = new AssemblySimulator();

    @Test
    void should_move_100_to_eax() {
        as.reset();
        as.memory[0] = "movl $100, %eax";
        as.run();
        assertEquals(100, as.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_ebx_to_eax() {
        as.reset();
        as.memory[0] = "movl $100, %ebx";
        as.memory[1] = "movl %ebx, %eax";
        as.run();
        assertEquals(100, as.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_5_address_to_eax() {
        as.reset();
        as.memory[5] = "100"; // load data
        as.memory[0] = "movl 5, %eax";
        as.run();
        assertEquals(100, as.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_ebx_pointer_to_eax() {
        as.reset();
        as.memory[5] = "100"; // load data
        as.memory[0] = "movl $5, %ebx";
        as.memory[1] = "movl (%ebx), %eax";
        as.run();
        assertEquals(100, as.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_5_indexed_address_to_eax() {
        as.reset();
        as.memory[5] = "100"; // load data
        as.memory[0] = "movl $1, %ebx";
        as.memory[1] = "movl 4(,%ebx,), %eax";
        as.run();
        assertEquals(100, as.cpuRegisters.get("%eax"));
    }

    @Test
    void should_move_value_at_ebx_indexed_address_to_eax() {
        as.reset();
        as.memory[5] = "100"; // load data
        as.memory[0] = "movl $4, %ebx";
        as.memory[1] = "movl 1(%ebx), %eax";
        as.run();
        assertEquals(100, as.cpuRegisters.get("%eax"));
    }

}