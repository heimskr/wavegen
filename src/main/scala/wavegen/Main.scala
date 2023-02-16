package wavegen

import chisel3._
import chisel3.stage._

class Main extends Module {
	implicit val clockFreq = 100_000_000

	val io = IO(new Bundle {
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val sw      = Input(UInt(8.W))
		val outL    = Output(UInt(24.W))
		val outR    = Output(UInt(24.W))
		val led     = Output(UInt(8.W))
	})

	val freq = io.sw(7, 1) << 4.U

	val square = Module(new SquareGen(24))
	square.io.pause := !io.sw(0)
	square.io.freq := freq
	square.io.max := "h0fffff".U
	square.io.wave := "b10".U

	when (io.sw(0)) {
		io.outL := square.io.out
		io.outR := square.io.out
	} .otherwise {
		io.outL := 0.U
		io.outR := 0.U
	}

	io.led := freq(7, 0)
}

object MainRun extends scala.App {
	(new ChiselStage).emitVerilog(new Main, args)
}
