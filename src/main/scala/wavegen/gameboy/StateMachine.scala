package wavegen.gameboy

import chisel3._
import chisel3.util._

class StateMachine(data: Seq[Byte]) extends Module {
	val io = IO(new Bundle {
		val start     = Input(Bool())
		val tick      = Input(Bool())
		val state     = Output(UInt(4.W))
		val error     = Output(UInt(4.W))
		val errorInfo = Output(UInt(8.W))
		val registers = Output(Registers())
	})

	def get4(index: UInt): UInt = Cat(rom(index + 3.U), rom(index + 2.U), rom(index + 1.U), rom(index))
	def get4(index: Int):  UInt = get4(index.U(32.W))
	def get2(index: UInt): UInt = Cat(rom(index + 1.U), rom(index))
	def get2(index: Int):  UInt = get2(index.U(32.W))
	def get(index: UInt):  UInt = rom(index)
	def get(index: Int):   UInt = get(index.U(32.W))

	def setReg(index: UInt, value: UInt): Unit = {
		val failed = WireDefault(true.B)

		switch (index) {
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
			error := eBadReg
		}
	}

	val sIdle :: sInit :: sPlaying :: sWaiting :: sDone :: Nil = Enum(5)
	val eNone :: eBadReg :: eInvalidOpcode :: eUnimplemented :: Nil = Enum(4)

	val rom = VecInit(data.map { _.S(8.W).asUInt })
	
	val state = RegInit(sInit)
	val error = RegInit(eNone)
	val pointer = RegInit(0.U(32.W))

	val registers = RegInit(0.U.asTypeOf(Registers()))

	// val counterEnable = RegInit(false.B)
	// val counterReset  = RegInit(true.B)
	// val (waitCounter, waitWrap) = Counter(0 until 65536, counterEnable, counterReset)
	val waitCounter = RegInit(0.U(32.W))

	val errorInfo = RegInit(0.U(8.W))

	when (error === eNone && io.tick) {
		when (state === sIdle) {
			when (io.start) {
				state := sInit
			}
		} .elsewhen (state === sInit) {
			registers := 0.U.asTypeOf(Registers())
			pointer := get4(0x34) + "h34".U
			state := sPlaying
		} .elsewhen (state === sPlaying) {
			val opcode = WireDefault(get(pointer))
			val failed = WireDefault(true.B)

			switch (opcode) {
				is("hb3".U) {
					val regID = Wire(UInt(8.W))
					val byte  = Wire(UInt(8.W))
					regID  := get(pointer + 1.U)
					byte   := get(pointer + 2.U)
					setReg(get(pointer + 1.U), get(pointer + 2.U))
					pointer := pointer + 3.U
					failed  := false.B
				}

				is ("h61".U) {
					waitCounter := get2(pointer + 1.U)
					state       := sWaiting
					pointer     := pointer + 1.U
					failed      := false.B
				}

				is ("h66".U) {
					state  := sDone
					failed := false.B
				}

				is ("h67".U) {
					error     := eUnimplemented
					errorInfo := "h67".U
					failed    := false.B // :^)
				}
			}

			when ("h70".U <= opcode && opcode <= "h7f".U) {
				waitCounter := opcode - "h6f".U
				state       := sWaiting
				pointer     := pointer + 1.U
				failed      := false.B
			}

			when (failed) {
				error     := eInvalidOpcode
				errorInfo := opcode
			}
		} .elsewhen (state === sWaiting) {
			when (waitCounter === 0.U) {
				state := sPlaying
			} .otherwise {
				waitCounter := waitCounter - 1.U
			}
		} .elsewhen (state === sDone) {
			when (io.start) {
				state := sInit
			}
		}
	}

	io.state := state
	io.error := error
	io.errorInfo := errorInfo
	io.registers := registers
}
