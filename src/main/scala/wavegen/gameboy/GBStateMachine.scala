package wavegen.gameboy

import chisel3._
import chisel3.util._

class GBStateMachine(addressWidth: Int, romWidth: Int)(implicit inSimulator: Boolean) extends Module {
	val io = IO(new Bundle {
		val start           = Input(Bool())
		val tick            = Input(Bool())
		val rom             = Input(UInt(romWidth.W))
		val state           = Output(UInt(4.W))
		val error           = Output(UInt(4.W))
		val errorInfo       = Output(UInt(8.W))
		val errorInfo2      = Output(UInt(16.W))
		val errorInfo3      = Output(UInt(8.W))
		val registers       = Output(GBRegisters())
		val addr            = Output(UInt(addressWidth.W))
		val channelsEnabled = Output(UInt(4.W))
		val info            = Output(UInt(8.W))
		val adjusted        = Output(UInt(8.W))
		val value           = Output(UInt(8.W))
		val opcode          = Output(UInt(8.W))
		val operand1        = Output(UInt(8.W))
		val operand2        = Output(UInt(8.W))
		val pointer         = Output(UInt(addressWidth.W))
		val waitCounter     = Output(UInt(32.W))
		val nr13In          = Flipped(Valid(UInt(8.W)))
		val nr14In          = Flipped(Valid(UInt(8.W)))
	})

	val adjustedReg     = RegInit(0.U(8.W))
	val valueReg        = RegInit(0.U(8.W))
	val channelsEnabled = RegInit(0.U(4.W)) // {ch4, ch3, ch2, ch1}

	def setChannel(channel: Int, value: Boolean): Unit = {
		val bit = if (value) 1.U else 0.U
		if (channel == 1)
			channelsEnabled := Cat(channelsEnabled(3, 1), bit)
		else if (channel == 4)
			channelsEnabled := Cat(bit, channelsEnabled(2, 0))
		else
			channelsEnabled := Cat(channelsEnabled(3, channel), bit, channelsEnabled(channel - 2, 0))
	}

	def setReg(index: UInt, value: UInt): Unit = {
		val adjusted = index + "h10".U
		printf(cf"setReg (pointer = 0x$pointer%x): *0xff$adjusted%x (unadjusted: 0x$index%x) = 0x$value%x\n")
		val failed = WireDefault(true.B)

		adjustedReg := adjusted
		valueReg    := value

		switch (adjusted) {
			is("h10".U) { registers.NR10 := value; failed := false.B }
			is("h11".U) { registers.NR11 := value; failed := false.B }
			is("h12".U) { failed := false.B
				registers.NR12   := value
				when (value(7, 3) === 0.U) {
					setChannel(1, false)
				}
			}
			is("h13".U) { registers.NR13 := value; failed := false.B }
			is("h14".U) { failed := false.B
				registers.NR14   := value
				when (value(7)) {
					setChannel(1, true)
				}
			}
			is("h16".U) { registers.NR21 := value; failed := false.B }
			is("h17".U) { failed := false.B
				registers.NR22   := value
				when (value(7, 3) === 0.U) {
					setChannel(2, false)
				}
			}
			is("h18".U) { registers.NR23 := value; failed := false.B }
			is("h19".U) { failed := false.B
				registers.NR24   := value
				when (value(7)) {
					setChannel(2, true)
				}
			}
			is("h1a".U) { registers.NR30 := value; failed := false.B }
			is("h1b".U) { registers.NR31 := value; failed := false.B }
			is("h1c".U) { registers.NR32 := value; failed := false.B }
			is("h1d".U) { registers.NR33 := value; failed := false.B }
			is("h1e".U) { registers.NR34 := value; failed := false.B }
			is("h20".U) { registers.NR41 := value; failed := false.B }
			is("h21".U) { failed := false.B
				registers.NR42   := value
				when (value(7, 3) === 0.U) {
					setChannel(4, false)
				}
			}
			is("h22".U) { registers.NR43 := value; failed := false.B }
			is("h23".U) { failed := false.B
				registers.NR44   := value
				when (value(7) === 0.U) {
					setChannel(4, true)
				}
			}
			is("h24".U) { registers.NR50 := value; failed := false.B }
			is("h25".U) { registers.NR51 := value; failed := false.B }
			is("h26".U) { failed := false.B
				channelsEnabled  := Fill(4, value(7))
				registers.NR52   := Cat(value(7), 0.U(3.W), channelsEnabled)
			}
			is("h30".U) { registers.WT0  := value; failed := false.B }
			is("h31".U) { registers.WT1  := value; failed := false.B }
			is("h32".U) { registers.WT2  := value; failed := false.B }
			is("h33".U) { registers.WT3  := value; failed := false.B }
			is("h34".U) { registers.WT4  := value; failed := false.B }
			is("h35".U) { registers.WT5  := value; failed := false.B }
			is("h36".U) { registers.WT6  := value; failed := false.B }
			is("h37".U) { registers.WT7  := value; failed := false.B }
			is("h38".U) { registers.WT8  := value; failed := false.B }
			is("h39".U) { registers.WT9  := value; failed := false.B }
			is("h3a".U) { registers.WTA  := value; failed := false.B }
			is("h3b".U) { registers.WTB  := value; failed := false.B }
			is("h3c".U) { registers.WTC  := value; failed := false.B }
			is("h3d".U) { registers.WTD  := value; failed := false.B }
			is("h3e".U) { registers.WTE  := value; failed := false.B }
			is("h3f".U) { registers.WTF  := value; failed := false.B }
		}

		when (failed) {
			printf(cf"Bad reg: 0xff$adjusted%x\n")
			error := eBadReg
			errorInfo  := adjusted
			errorInfo2 := pointer(15, 0)
		}
	}

	val sIdle :: sGetOpcode :: sOperate :: sWaiting :: sDone :: Nil = Enum(5)
	val eNone :: eBadReg :: eInvalidOpcode :: eUnimplemented :: eBadSubpointer :: Nil = Enum(5)

	val state       = RegInit(sIdle)
	val error       = RegInit(eNone)
	val pointer     = RegInit(0.U(addressWidth.W))
	val registers   = RegInit(0.U.asTypeOf(GBRegisters()))
	val waitCounter = RegInit(0.U(32.W))
	val errorInfo   = RegInit(0.U(8.W))
	val errorInfo2  = RegInit(0.U(16.W))
	val errorInfo3  = RegInit(0.U(8.W))
	val opcode      = RegInit(0.U(8.W))
	val operand1    = RegInit(0.U(8.W))
	val operand2    = RegInit(0.U(8.W))
	val tempByte    = RegInit(0.U(8.W))
	val subpointer  = RegInit(0.U(3.W))

	def badSubpointer(): Unit = { error := eBadSubpointer; errorInfo := opcode }
	def advance(): Unit = { pointer := pointer + 1.U }

	// def toCycles(samples: UInt): UInt = samples * (clockFreq / 44100).U
	// def toCycles(samples: UInt) = (samples << 11.U) + (samples << 7.U) + (samples << 6.U) + (samples << 4.U) + (samples << 3.U) + (samples << 2.U)
	def toCycles(samples: UInt) = (samples << 6.U) + (samples << 4.U) + (samples << 3.U) + (samples << 2.U) + (samples << 1.U) + samples

	io.info := 1.U

	when (!io.tick) {
		io.info := 24.U
	} .otherwise {

		// Disable triggers
		registers.NR14 := Cat(0.U(1.W), registers.NR14(6, 0))
		registers.NR24 := Cat(0.U(1.W), registers.NR24(6, 0))
		registers.NR34 := Cat(0.U(1.W), registers.NR34(6, 0))
		registers.NR44 := Cat(0.U(1.W), registers.NR44(6, 0))

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
						failed := false.B
						setReg(operand1, operand2)
						advance()
						state := sGetOpcode
					}

					is ("h91".U) {
						io.info := 14.U
						failed := false.B
						val toWait = if (inSimulator) 2.U else toCycles(Cat(operand2, operand1))
						printf(cf"Waiting 0x$toWait%x cycles around 0x${pointer}%x (samples: ${Cat(operand2, operand1)}).\n")
						waitCounter := toWait
						advance()
						state := sWaiting
					}

					is ("h92".U) {
						io.info   := 15.U
						state     := sDone
						registers := 0.U.asTypeOf(GBRegisters())
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

	when (io.nr13In.valid) {
		registers.NR13 := io.nr13In.bits
	}

	when (io.nr14In.valid) {
		registers.NR14 := io.nr14In.bits
	}

	io.addr            := pointer
	io.state           := state
	io.error           := error
	io.errorInfo       := errorInfo
	io.errorInfo2      := errorInfo2
	io.errorInfo3      := errorInfo3
	io.registers       := registers
	io.adjusted        := adjustedReg
	io.value           := valueReg
	io.opcode          := opcode
	io.operand1        := operand1
	io.operand2        := operand2
	io.pointer         := pointer
	io.waitCounter     := waitCounter
	io.channelsEnabled := channelsEnabled
}
