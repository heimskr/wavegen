package wavegen.nes

import chisel3._
import chisel3.util._

class TriangleChannel extends Module {
	val io = IO(new ChannelIO {
		val writes = Input(TriangleWrites())
	})

	val control      = io.registers.$4008(7) // Also the length counter halt flag
	val reloadValue  = io.registers.$4008(6, 0)
	val lengthLoad   = io.registers.$400B(7, 3)
	val enableLength = io.registers.$4015(3)
	val timerValue   = Cat(io.registers.$400B(2, 0), io.registers.$400A)

	val counter = RegInit(0.U(11.W))
	val steps   = VecInit(15.U(4.W), 14.U(4.W), 13.U(4.W), 12.U(4.W), 11.U(4.W), 10.U(4.W), 9.U(4.W), 8.U(4.W), 7.U(4.W), 6.U(4.W), 5.U(4.W), 4.U(4.W), 3.U(4.W), 2.U(4.W), 1.U(4.W), 0.U(4.W), 0.U(4.W), 1.U(4.W), 2.U(4.W), 3.U(4.W), 4.U(4.W), 5.U(4.W), 6.U(4.W), 7.U(4.W), 8.U(4.W), 9.U(4.W), 10.U(4.W), 11.U(4.W), 12.U(4.W), 13.U(4.W), 14.U(4.W), 15.U(4.W))
	val step    = RegInit(0.U(5.W))

	val linearCounter = RegInit(0.U(7.W))

	val lengthCounter = Module(new LengthCounter(3))
	lengthCounter.io.ticks     := io.ticks
	lengthCounter.io.registers := io.registers
	lengthCounter.io.loadValue := io.registers.$400B(7, 3)
	lengthCounter.io.write     := io.writes.counterReload // TODO: verify
	lengthCounter.io.halt      := control

	val counterReloadFlag = RegInit(false.B)
	val counterReloadNow  = io.writes.counterReload

	when (counterReloadNow) {
		counterReloadFlag := true.B
	}

	when (io.ticks.quarter) {
		when (counterReloadNow || counterReloadFlag) {
			linearCounter := reloadValue
		} .elsewhen (linearCounter =/= 0.U) {
			linearCounter := linearCounter - 1.U
		}

		when (!control) {
			counterReloadFlag := false.B
		}
	}

	when (io.ticks.cpu && linearCounter =/= 0.U && lengthCounter.io.out =/= 0.U) {
		when (counter === 0.U) {
			counter := timerValue
			step    := step + 1.U
		} .otherwise {
			counter := counter - 1.U
		}
	}

	io.out := steps(step)
}
