package wavegen.nes

import chisel3._
import chisel3.util._

class NESStateMachine(addressWidth: Int, romWidth: Int)(implicit inSimulator: Boolean) extends Module {
	val io = IO(new Bundle {
		val start           = Input(Bool())
		val tick            = Input(Bool())
		val rom             = Input(UInt(romWidth.W))
		val state           = Output(UInt(4.W))
		val error           = Output(UInt(4.W))
		val errorInfo       = Output(UInt(8.W))
		val errorInfo2      = Output(UInt(16.W))
		val errorInfo3      = Output(UInt(8.W))
		val registers       = Output(NESRegisters())
		val addr            = Output(UInt(addressWidth.W))
		val channelsEnabled = Output(UInt(4.W))
		val info            = Output(UInt(8.W))
		val opcode          = Output(UInt(8.W))
		val operand1        = Output(UInt(8.W))
		val operand2        = Output(UInt(8.W))
		val pointer         = Output(UInt(addressWidth.W))
		val waitCounter     = Output(UInt(32.W))
		// val nr13In          = Flipped(Valid(UInt(8.W)))
	})

	val channelsEnabled = RegInit(0.U(5.W)) // {ch5, ch4, ch3, ch2, ch1}

	def setChannel(channel: Int, value: Boolean): Unit = {
		if (channel == 1)
			channelsEnabled := Cat(channelsEnabled(4, 1), value.B)
		else if (channel == 5)
			channelsEnabled := Cat(value.B, channelsEnabled(3, 0))
		else
			channelsEnabled := Cat(channelsEnabled(4, channel), value.B, channelsEnabled(channel - 2, 0))
	}

	def setReg(index: UInt, value: UInt): Unit = {
		printf(cf"setReg (pointer = 0x$pointer%x): *0x$index%x = 0x$value%x\n")
		val failed = WireDefault(true.B)

		def write(to: Data): Unit = { failed := false.B; to := value }

		switch (index) {
			is ("h00".U) { write(registers.$4000) }
			is ("h01".U) { write(registers.$4001) }
	 		is ("h02".U) { write(registers.$4002) }
	 		is ("h03".U) { write(registers.$4003) }
	 		is ("h04".U) { write(registers.$4004) }
	 		is ("h05".U) { write(registers.$4005) }
	 		is ("h06".U) { write(registers.$4006) }
	 		is ("h07".U) { write(registers.$4007) }
	 		is ("h08".U) { write(registers.$4008) }
	 		is ("h0A".U) { write(registers.$400A) }
	 		is ("h0B".U) { write(registers.$400B) }
	 		is ("h0C".U) { write(registers.$400C) }
	 		is ("h0E".U) { write(registers.$400E) }
	 		is ("h0F".U) { write(registers.$400F) }
	 		is ("h10".U) { write(registers.$4010) }
	 		is ("h11".U) { write(registers.$4011) }
	 		is ("h12".U) { write(registers.$4012) }
	 		is ("h13".U) { write(registers.$4013) }
	 		is ("h15".U) { write(registers.$4015) }
	 		is ("h17".U) { write(registers.$4017) }
		}

		when (failed) {
			printf(cf"Bad reg: 0x$index%x\n")
			error := eBadReg
			errorInfo  := index
			errorInfo2 := pointer(15, 0)
		}
	}

	val sIdle :: sGetOpcode :: sOperate       :: sWaiting       :: sDone          :: Nil = Enum(5)
	val eNone :: eBadReg    :: eInvalidOpcode :: eUnimplemented :: eBadSubpointer :: Nil = Enum(5)

	val state       = RegInit(sIdle)
	val error       = RegInit(eNone)
	val pointer     = RegInit(0.U(addressWidth.W))
	val registers   = RegInit(0.U.asTypeOf(NESRegisters()))
	val waitCounter = RegInit(0.U(32.W))
	val errorInfo   = RegInit(0.U(8.W))
	val errorInfo2  = RegInit(0.U(16.W))
	val errorInfo3  = RegInit(0.U(8.W))
	val opcode      = RegInit(0.U(8.W))
	val operand1    = RegInit(0.U(8.W))
	val operand2    = RegInit(0.U(8.W))
	val tempByte    = RegInit(0.U(8.W))
	val subpointer  = RegInit(0.U(3.W))

	def badSubpointer(): Unit   = { error := eBadSubpointer; errorInfo := opcode }
	def advance():       Unit   = { pointer := pointer + 1.U }
	def toCycles(samples: UInt) = (samples << 5.U) + (samples << 2.U) + samples

	io.info := 1.U

	when (!io.tick) {
		io.info := 24.U
	} .otherwise {
		when (error === eNone) {
			when (state === sIdle) {
				when (io.start) {
					pointer := 0.U
					state   := sGetOpcode
					waitCounter := 0.U
					io.info := 3.U
				} .otherwise {
					io.info := 4.U
				}
			} .elsewhen (state === sGetOpcode) {
				when (waitCounter === 0.U) {
					io.info    := 9.U
					opcode     := io.rom(23, 16)
					operand1   := io.rom(15,  8)
					operand2   := io.rom( 7,  0)
					state      := sOperate
					subpointer := 0.U
				} .otherwise {
					io.info := 11.U
					waitCounter := waitCounter - 1.U
				}
			} .elsewhen (state === sOperate && subpointer =/= 0.U) {
				subpointer := subpointer - 1.U
			} .elsewhen (state === sOperate) {
				io.info := 12.U

				val failed = WireDefault(true.B)

				switch (opcode) {
					is("h90".U) {
						io.info := 13.U
						failed  := false.B
						setReg(operand1, operand2)
						advance()
						state := sGetOpcode
					}

					is ("h91".U) {
						io.info := 14.U
						failed  := false.B
						val toWait = if (inSimulator) 2.U else toCycles(Cat(operand2, operand1))
						printf(cf"Waiting 0x$toWait%x cycles around 0x${pointer}%x (samples: ${Cat(operand2, operand1)}).\n")
						waitCounter := toWait
						advance()
						state := sWaiting
					}

					is ("h92".U) {
						io.info   := 15.U
						state     := sDone
						registers := 0.U.asTypeOf(NESRegisters())
						failed    := false.B
						errorInfo := "b01010101".U
						printf(cf"Finishing with 0x92 around $pointer.\n")
						Seq.tabulate(4)(c => setChannel(c + 1, false))
					}
				}

				when (failed) {
					printf(cf"Bad opcode: 0x$opcode%x around 0x${pointer}%x\n")
					io.info    := 18.U
					error      := eInvalidOpcode
					errorInfo  := opcode
					errorInfo2 := pointer(15, 0)
					errorInfo3 := Cat(0.U(6.W), pointer(17, 16))
				}
			} .elsewhen (state === sWaiting) {
				io.info := 19.U
				when (waitCounter === 0.U) {
					io.info := 21.U
					state := sGetOpcode
				} .otherwise {
					io.info := 22.U
					waitCounter := waitCounter - 1.U
				}
			}
		} .otherwise {
			io.info := 2.U
		}
	}

	// TODO: handle writes
	// when (io.nr13In.valid) {
	// 	registers.NR13 := io.nr13In.bits
	// }

	io.addr            := pointer
	io.state           := state
	io.error           := error
	io.errorInfo       := errorInfo
	io.errorInfo2      := errorInfo2
	io.errorInfo3      := errorInfo3
	io.registers       := registers
	io.opcode          := opcode
	io.operand1        := operand1
	io.operand2        := operand2
	io.pointer         := pointer
	io.waitCounter     := waitCounter
	io.channelsEnabled := channelsEnabled
}
