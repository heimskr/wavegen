package wavegen.nes

import chisel3._
import chisel3.util._

class LengthCounter(channelID: Int) extends Module {
	override val desiredName = "NESLengthCounter"

	val io = IO(new Bundle {
		val ticks     = Input(Ticks())
		val write     = Input(Bool())
		val halt      = Input(Bool())
		val registers = Input(NESRegisters())
		val loadValue = Input(UInt(5.W))
		val out       = Output(UInt(8.W))
	})

	val enableLength  = io.registers.$4015(channelID - 1)
	val lengthTable   = VecInit(10.U(8.W), 254.U(8.W), 20.U(8.W), 2.U(8.W), 40.U(8.W), 4.U(8.W), 80.U(8.W), 6.U(8.W), 160.U(8.W), 8.U(8.W), 60.U(8.W), 10.U(8.W), 14.U(8.W), 12.U(8.W), 26.U(8.W), 14.U(8.W), 12.U(8.W), 16.U(8.W), 24.U(8.W), 18.U(8.W), 48.U(8.W), 20.U(8.W), 96.U(8.W), 22.U(8.W), 192.U(8.W), 24.U(8.W), 72.U(8.W), 26.U(8.W), 16.U(8.W), 28.U(8.W), 32.U(8.W), 30.U(8.W))
	val lengthCounter = RegInit(0.U(8.W))

	val writeReg = RegInit(false.B)

	when (io.write) {
		writeReg := true.B
	}

	when (io.ticks.half) {
		when (!enableLength) {
			lengthCounter := 0.U
		} .elsewhen (io.write || writeReg) {
			lengthCounter := lengthTable(io.loadValue)
			writeReg      := false.B
		} .elsewhen (!io.halt && lengthCounter =/= 0.U) {
			lengthCounter := lengthCounter - 1.U
		}
	}

	io.out := lengthCounter
}
