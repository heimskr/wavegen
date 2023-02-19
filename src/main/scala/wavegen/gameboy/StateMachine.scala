package wavegen.gameboy

import chisel3._
import chisel3.util._

class StateMachine(tickFreq: Int) extends Module {
	val io = IO(new Bundle {
		val start      = Input(Bool())
		val tick       = Input(Bool())
		val pause      = Input(Bool())
		val rom        = Input(UInt(8.W))
		val state      = Output(UInt(4.W))
		val error      = Output(UInt(4.W))
		val errorInfo  = Output(UInt(8.W))
		val errorInfo2 = Output(UInt(16.W))
		val errorInfo3 = Output(UInt(8.W))
		val registers  = Output(Registers())
		val addr       = Output(UInt(18.W))
		val info       = Output(UInt(8.W))
	})

	def setReg(index: UInt, value: UInt): Unit = {
		val adjusted = index + "h10".U
		printf(cf"setReg (pointer = 0x$pointer%x): *0xff$adjusted%x (unadjusted: 0x$index%x) = 0x$value%x\n")
		val failed = WireDefault(true.B)

		switch (adjusted) {
			is("h10".U) { registers.NR10 := value; failed := false.B }
			is("h11".U) { registers.NR11 := value; failed := false.B }
			is("h12".U) { registers.NR12 := value; failed := false.B }
			is("h13".U) { registers.NR13 := value; failed := false.B }
			is("h14".U) { registers.NR14 := value; failed := false.B }
			is("h16".U) { registers.NR21 := value; failed := false.B }
			is("h17".U) { registers.NR22 := value; failed := false.B }
			is("h18".U) { registers.NR23 := value; failed := false.B }
			is("h19".U) { registers.NR24 := value; failed := false.B }
			is("h1a".U) { registers.NR30 := value; failed := false.B }
			is("h1b".U) { registers.NR31 := value; failed := false.B }
			is("h1c".U) { registers.NR32 := value; failed := false.B }
			is("h1d".U) { registers.NR33 := value; failed := false.B }
			is("h1e".U) { registers.NR34 := value; failed := false.B }
			is("h20".U) { registers.NR41 := value; failed := false.B }
			is("h21".U) { registers.NR42 := value; failed := false.B }
			is("h22".U) { registers.NR43 := value; failed := false.B }
			is("h23".U) { registers.NR44 := value; failed := false.B }
			is("h24".U) { registers.NR50 := value; failed := false.B }
			is("h25".U) { registers.NR51 := value; failed := false.B }
			is("h26".U) { registers.NR52 := value; failed := false.B }
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

	val sIdle :: sInit :: sGetOpcode :: sOperate :: sWaiting :: Nil = Enum(5)
	val eNone :: eBadReg :: eInvalidOpcode :: eUnimplemented :: eBadSubpointer :: Nil = Enum(5)

	val state       = RegInit(sIdle)
	val error       = RegInit(eNone)
	val pointer     = RegInit(0.U(18.W))
	val registers   = RegInit(0.U.asTypeOf(Registers()))
	val waitCounter = RegInit(0.U(32.W))
	val errorInfo   = RegInit(0.U(8.W))
	val errorInfo2  = RegInit(0.U(16.W))
	val errorInfo3  = RegInit(0.U(8.W))
	val opcode      = RegInit(0.U(8.W))
	val four        = RegInit(VecInit(Seq.fill(4)(0.U(8.W))))
	val tempByte    = RegInit(0.U(8.W))
	val subpointer  = RegInit(0.U(3.W))

	def badSubpointer(): Unit = { error := eBadSubpointer; errorInfo := opcode }

	def toCycles(samples: UInt): UInt = samples * (tickFreq / 44100).U

	io.info := 1.U

	when (io.pause) {
		io.info := 24.U
	} .elsewhen (error === eNone) {
		when (state === sIdle) {
			when (io.start) {
				pointer := "h34".U
				state   := sInit
				waitCounter := 0.U
				io.info := 3.U
			} .otherwise {
				io.info := 4.U
			}
		} .elsewhen (state === sInit) {
			io.info := 23.U
			when (waitCounter === 0.U) {
				io.info := 5.U
				registers := 0.U.asTypeOf(Registers())
				four(subpointer) := io.rom
				pointer := pointer + 1.U
				when (subpointer === 3.U) {
					io.info := 6.U
					pointer := four.asUInt + "h34".U
					state   := sGetOpcode
					subpointer := 0.U
					waitCounter := 1.U
				} .otherwise {
					io.info := 7.U
					subpointer := subpointer + 1.U
				}
			} .otherwise {
				io.info := 8.U
				waitCounter := waitCounter - 1.U
				pointer := pointer + 1.U
			}
		} .elsewhen (state === sGetOpcode) {
			when (waitCounter === 0.U) {
				io.info := 9.U
				opcode := io.rom
				when (io.tick) {
					io.info := 10.U
					state   := sOperate
					pointer := pointer + 1.U
				}
			} .otherwise {
				io.info := 11.U
				waitCounter := waitCounter - 1.U
			}
		} .elsewhen (state === sOperate) {
			io.info := 12.U

			val failed = WireDefault(true.B)

			switch (opcode) {
				is("hb3".U) {
					io.info := 13.U
					failed := false.B
					when (subpointer === 0.U) {
						subpointer := 1.U
					} .elsewhen (subpointer === 1.U) {
						tempByte := io.rom
						pointer := pointer + 1.U
						subpointer := 2.U
					} .elsewhen (subpointer === 2.U) {
						setReg(tempByte, io.rom)
						subpointer := 3.U
						pointer := pointer + 1.U
						subpointer := 0.U
						state := sGetOpcode
					// } .elsewhen (subpointer === 3.U) {
					// 	subpointer := 0.U
					// 	state := sGetOpcode
					} .otherwise {
						badSubpointer()
					}
				}

				is ("h61".U) {
					io.info := 14.U
					failed := false.B
					when (subpointer === 0.U) {
						tempByte   := io.rom
						pointer    := pointer + 1.U
						subpointer := 1.U
					} .elsewhen (subpointer === 1.U) {
						waitCounter := toCycles(Cat(io.rom, tempByte))
						printf(cf"Waiting 0x${toCycles(Cat(io.rom, tempByte))}%x cycles around 0x${pointer - 1.U}%x.\n")
						subpointer  := 0.U
						pointer     := pointer + 1.U
						state       := sWaiting
					} .otherwise {
						badSubpointer()
					}
				}

				is ("h66".U) {
					io.info   := 15.U
					state     := sIdle
					failed    := false.B
					errorInfo := "b01010101".U
				}

				is ("h67".U) {
					io.info   := 16.U
					error     := eUnimplemented
					errorInfo := "h67".U
					failed    := false.B // :^)
				}
			}

			when ("h70".U <= opcode && opcode <= "h7f".U) {
				io.info := 17.U
				waitCounter := opcode - "h6f".U
				state       := sWaiting
				// pointer     := pointer + 1.U
				failed      := false.B
				errorInfo2  := opcode - "h6f".U
			}

			when (failed) {
				printf(cf"Bad opcode: 0x$opcode%x around 0x${pointer - 1.U}%x\n")
				io.info := 18.U
				error   := eInvalidOpcode

				val actualAddress = pointer - 1.U
				errorInfo  := opcode
				errorInfo2 := actualAddress(15, 0)
				errorInfo3 := Cat(0.U(6.W), actualAddress(17, 16))
			}
		} .elsewhen (state === sWaiting) {
			io.info := 19.U
			when (io.tick) {
				io.info := 20.U
				when (waitCounter === 0.U) {
					io.info := 21.U
					state := sGetOpcode
				} .otherwise {
					io.info := 22.U
					waitCounter := waitCounter - 1.U
				}
			}
		}
	} .otherwise {
		io.info := 2.U
	}

	io.addr := pointer
	io.state := state
	io.error := error
	io.errorInfo  := errorInfo
	io.errorInfo2 := errorInfo2
	io.errorInfo3 := errorInfo3
	io.registers := registers
}
