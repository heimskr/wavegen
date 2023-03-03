package wavegen

import chisel3._
import chisel3.util._
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
		val rom     = Input(UInt(8.W))
		val outL    = Output(UInt(24.W))
		val outR    = Output(UInt(24.W))
		val led     = Output(UInt(8.W))
		val addr    = Output(UInt(18.W))
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
	io.addr := DontCare
}

class MainROMReader extends Module {
	implicit val clockFreq: Int = 100_000_000

	val io = IO(new Bundle {
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val sw      = Input(UInt(8.W))
		val rom     = Input(UInt(24.W))
		val outL    = Output(UInt(24.W))
		val outR    = Output(UInt(24.W))
		val led     = Output(UInt(8.W))
		val addr    = Output(UInt(17.W))
	})

	io.addr := io.sw << Cat(io.buttonL, io.buttonR)

	when (io.buttonU) {
		io.led := io.rom(23, 16)
	} .elsewhen (io.buttonD) {
		io.led := io.rom(15, 8)
	} .otherwise {
		io.led := io.rom(7, 0)
	}

	io.outL := 0.U
	io.outR := 0.U
}

class MainGameBoy extends Module {
	// implicit val clockFreq   = 100_000_000
	implicit val clockFreq   = 100_000_000
	implicit val inSimulator = false
	val addressWidth = 18
	val romWidth = 24

	val io = IO(new Bundle {
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val sw      = Input(UInt(8.W))
		val rom     = Input(UInt(romWidth.W))
		val outL    = Output(UInt(24.W))
		val outR    = Output(UInt(24.W))
		val led     = Output(UInt(8.W))
		val addr    = Output(UInt(addressWidth.W))
	})

	var centerReg = RegInit(false.B)

	val start = WireDefault(false.B)

	when (io.buttonC && !centerReg) {
		centerReg := true.B
		start     := true.B
	} .otherwise {
		centerReg := io.buttonC
		start     := io.buttonC
	}

	val gameboy = Module(new wavegen.gameboy.GameBoy(addressWidth, romWidth))
	gameboy.io.start := io.buttonC
	gameboy.io.sw    := io.sw

	// def increase7to24(value: UInt, boost: Int = 2): UInt = (value << (14 + boost).U) | (value << (7 + boost).U) | (value << boost.U)
	def increase7to24(value: UInt): UInt = value << 16.U
	def increase8to24(value: UInt): UInt = value << 15.U
	// def increase9to24(value: UInt): UInt = value << (8.U +& io.sw(5, 3))
	def increase9to24(value: UInt): UInt = value << 11.U
	// def increase9to16(value: UInt): UInt = value << (1.U + io.sw(0) - io.sw(1))

	// io.outL := increase9to16(gameboy.io.outL)
	// io.outR := increase9to16(gameboy.io.outR)
	io.outL := increase9to24(gameboy.io.outL)
	io.outR := increase9to24(gameboy.io.outR)
	io.led  := gameboy.io.leds
	// io.led  := Cat(0.U(3.W), io.buttonU, io.buttonR, io.buttonD, io.buttonL, io.buttonC)
	io.addr := gameboy.io.addr
	gameboy.io.rom := io.rom
	gameboy.io.buttonD := io.buttonD
	gameboy.io.buttonU := io.buttonU
	gameboy.io.buttonL := io.buttonL
	gameboy.io.buttonR := io.buttonR
	gameboy.io.buttonC := io.buttonC
}

object MainRun extends scala.App {
	// (new ChiselStage).emitVerilog(new Main,  args)
	(new ChiselStage).emitVerilog(new MainGameBoy, args)
	(new ChiselStage).emitVerilog(new wavegen.misc.ImageOutput, args)
	// (new ChiselStage).emitVerilog(new MainROMReader, args)
	(new ChiselStage).emitVerilog(new Debouncer(5), args)
}
