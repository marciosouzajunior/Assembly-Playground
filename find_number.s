# prints the number if exists in list, otherwise prints 0

.section .data
	numbers_list:
	.long 1,2,3,4,5,6,7,8,9,10

	number_to_search:
	.long 5

.section .bss
	.lcomm charToPrint, 4

.section .text

.globl _start
_start:
	movl $0, %edi
	movl $0, %ecx

start_loop:
	cmpl $9, %edi
	je print_char
	movl numbers_list(,%edi,4), %eax
	incl %edi
	cmpl (number_to_search), %eax
	jne start_loop
	movl %eax, %ecx
	jmp print_char

loop_exit:
	movl $1, %eax
	int $0x80

print_char:
	# preparing to make a syscall, will print the value in ecx register
	movl $4, %eax			# sys_write
	movl $1, %ebx			# file descriptor (1 = screen)
	movl $1, %edx			# length of string

	#movl %edi, %ecx		# move the value to be printed
	addl $0x30, %ecx		# add 30 hex for ascii
	movl %ecx, [charToPrint]	# save in the buffer
	movl $charToPrint, %ecx		# save the adrress of buffer in ecx

	int $0x80
	jmp loop_exit
