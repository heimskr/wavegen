package wavegen.nes

import chisel3._
import chisel3.util._

class TriangleChannel extends Module {
	val io = IO(new ChannelIO)

	val control     = io.registers.$4008(7)
	val reloadValue = io.registers.$4008(6, 0)
	val lengthLoad  = io.registers.$400B(7, 3)
	val timerValue  = Cat(io.registers.$400B(2, 0), io.registers.$400A)

	val counter = RegInit(0.U(11.W))

	val steps = VecInit(15.U(4.W), 14.U(4.W), 13.U(4.W), 12.U(4.W), 11.U(4.W), 10.U(4.W), 9.U(4.W), 8.U(4.W), 7.U(4.W), 6.U(4.W), 5.U(4.W), 4.U(4.W), 3.U(4.W), 2.U(4.W), 1.U(4.W), 0.U(4.W), 0.U(4.W), 1.U(4.W), 2.U(4.W), 3.U(4.W), 4.U(4.W), 5.U(4.W), 6.U(4.W), 7.U(4.W), 8.U(4.W), 9.U(4.W), 10.U(4.W), 11.U(4.W), 12.U(4.W), 13.U(4.W), 14.U(4.W), 15.U(4.W))
	val step  = RegInit(0.U(5.W))

	when (io.ticks.cpu) {
		when (counter === 0.U) {
			counter := timerValue
			step    := step + 1.U
		} .otherwise {
			counter := counter - 1.U
		}
	}

	io.out := steps(step)
}
