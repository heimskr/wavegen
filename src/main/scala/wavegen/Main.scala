package wavegen

import chisel3._
import chisel3.stage._
import java.nio.file.{Files, Paths}

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

	// val freq = io.sw(6, 0) << 6.U
	val freq = 440.U

	val square = Module(new SquareGen(24))
	square.io.pause := !io.sw(7)
	square.io.freq  := freq << 1.U
	square.io.max   := "h0fffff".U
	square.io.wave  := "b10".U

	val sine = Module(new TableGen(new SineGenerator, 1024, 0x0fffff))
	sine.io.pause := io.sw(7)
	sine.io.freq  := freq

	when (io.sw(7)) {
		io.outL := square.io.out
		io.outR := square.io.out
	} .otherwise {
		io.outL := sine.io.out
		io.outR := sine.io.out
	}

	io.led := io.sw(3, 0)
}

class MainGameBoy(filename: String) extends Module {
	implicit val clockFreq: Int = 100_000_000

	val io = IO(new Bundle {
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val sw      = Input(UInt(8.W))
		val rom     = Input(UInt(8.W))
		val outL    = Output(UInt(24.W))
		val outR    = Output(UInt(24.W))
		val led     = Output(UInt(8.W))
		val addr    = Output(UInt(18.W))
	})

	var centerReg = RegInit(false.B)

	val start = WireDefault(false.B)

	when (io.buttonC && !centerReg) {
		centerReg := true.B
		start := true.B
	} .otherwise {
		centerReg := io.buttonC
	}

	val gameboy = Module(new wavegen.gameboy.GameBoy(Files.readAllBytes(Paths.get(filename))))
	gameboy.io.start := start

	val signal = gameboy.io.out << 16.U
	io.outL := signal
	io.outR := signal
	io.led  := 0.U
	io.addr := gameboy.io.addr
	gameboy.io.rom := io.rom
}

object MainRun extends scala.App {
	(new ChiselStage).emitVerilog(new MainGameBoy, args)
}
