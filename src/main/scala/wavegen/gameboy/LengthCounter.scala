// Credit: https://github.com/aselker/gameboy-sound-chip/blob/master/lenCounter.v

package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class LengthCounter(minuend: Int = 64) extends Module {
	override val desiredName = "GBLengthCounter"

	val width = log2Ceil(minuend)

	val io = IO(new Bundle {
		val tick      = Input(Bool()) // 256 Hz, please.
		val trigger   = Input(Bool())
		val enable    = Input(Bool())
		val loadValue = Input(UInt(width.W))
		val channelOn = Output(Bool())
	})

	val length    = RegInit(0.U((width + 1).W))
	val channelOn = RegInit(false.B)

	when (io.tick && io.enable && length =/= 0.U && !io.trigger) {
		length := length - 1.U
		channelOn := length =/= 0.U // TODO: verify register timing
	}

	when (io.trigger) {
		length := minuend.U - io.loadValue
		channelOn := true.B
	}

	io.channelOn := channelOn
}
