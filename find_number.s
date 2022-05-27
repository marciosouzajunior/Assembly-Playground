 .section .data

numbers_list:
 .long 1,2,3,4,5,6,7,8,9,10

StringToPrint:
 .string "Hello"

 .section .text

 .globl _start
_start:
 movl $0, %edi
 movl $0, %ebx

# TODO: not finished yet. need to load values from list and compare.
start_loop:
 cmpl $9, %edi
 jmp print_char
 je loop_exit
 mov $4, %ebx
 jmp loop_exit

loop_exit:
 movl $1, %eax
 int $0x80

# code from stack overflow ;)
# just for 'debug' porpuses

print_char:
movl $4, %eax
movl $1, %ebx
movl $StringToPrint, %ecx
movl $5, %edx
int $0x80
jmp loop_exit
