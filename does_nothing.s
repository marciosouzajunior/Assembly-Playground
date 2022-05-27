# these are directives and do not generate instructions
# they only direct the assembler to perform certain operations

# data is section to list any memory storage
.section .data

# text section is where the program instructions live
.section .text

# _start is a symbol that will be replaced by something else during assembly.
# symbols are used to mark locations of programs or data, so you can refer to them by name
# instead of number. is a special symbol that marks the location of the start of the program.
# .globl tells the assembler to not discard this symbol after assembly.
.globl _start

# this defines the value of _start symbol. it will make the value be the next instruction or data
_start:

# this is a instruction to move the value 1 to eax register
# the dollar sign indicates immediate mode addressing
# the reason to move is because we are preparing to do a system call (1 refers to exit call)
movl $1, %eax

# here we move 0 to ebx register. this is the value returned to the system
movl $0, %ebx

# int stands for interrupt and 0x80 is the interrupt number to use
# this will transfer the control from program to Linux
int $0x80
