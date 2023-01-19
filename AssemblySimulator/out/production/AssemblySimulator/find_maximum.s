.section .data
data_items:                             # this is a label that refers to location of first item
    .long 3,67,34,222,45,75,54,34,44,33,22,11,66,0

.section .text

 .globl _start
_start:
    movl $0, %edi			            # move 0 to index register
    movl data_items(,%edi,4), %eax	    # load first byte of data
    movl %eax, %ebx		                # first item is the biggest

start_loop:
    cmpl $0, %eax 			            # check if hit the end
    je loop_exit
    incl %edi			                # load next value
    movl data_items(,%edi,4), %eax
    cmpl %ebx, %eax		                # compare values
    jle start_loop			            # jump to loop beginning if the new is not bigger
    movl %eax, %ebx		                # move the value as the largest
    jmp start_loop

loop_exit:
    # %ebx is the status code for the exit system call
    # and it already has the maximum number
    movl $1, %eax			            # 1 is the exit() syscall
    int $0x80