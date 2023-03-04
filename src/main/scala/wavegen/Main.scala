package wavegen

import chisel3._
import chisel3.util._
import chisel3.stage._

class Main extends Module {
	implicit val clockFreq   = 100_000_000
	implicit val inSimulator = false
	val addressWidth = 18
	val romWidth = 24

	val io = IO(new Bundle {
		val pulseU = Input(Bool())
		val pulseR = Input(Bool())
		val pulseD = Input(Bool())
		val pulseL = Input(Bool())
		val pulseC = Input(Bool())
		val sw      = Input(UInt(8.W))
		val rom     = Input(UInt(romWidth.W))
		val outL    = Output(UInt(24.W))
		val outR    = Output(UInt(24.W))
		val led     = Output(UInt(8.W))
		val addr    = Output(UInt(addressWidth.W))
	})

	var centerReg = RegInit(false.B)

	val gameboy = Module(new wavegen.gameboy.GameBoy(addressWidth, romWidth))
	gameboy.io.start := io.pulseC
	gameboy.io.sw    := io.sw

	def increase9to24(value: UInt): UInt = value << 9.U

	val multiplierWidth = 4
	val multiplier = RegInit(4.U(multiplierWidth.W))
	val lastU = RegInit(false.B)
	val lastD = RegInit(false.B)

	when (io.pulseC && multiplier =/= ((1 << multiplierWidth) - 1).U) {
		multiplier := multiplier + 1.U
	}

	when (io.pulseD && multiplier =/= 0.U) {
		multiplier := multiplier - 1.U
	}

	io.outL := increase9to24(gameboy.io.outL) * multiplier
	io.outR := increase9to24(gameboy.io.outR) * multiplier
	io.led  := gameboy.io.leds
	io.addr := gameboy.io.addr
	gameboy.io.rom := io.rom
	gameboy.io.pulseD := io.pulseD
	gameboy.io.pulseU := io.pulseU
	gameboy.io.pulseL := io.pulseL
	gameboy.io.pulseR := io.pulseR
	gameboy.io.pulseC := io.pulseC
}

object MainRun extends scala.App {
	(new ChiselStage).emitVerilog(new Main, args)
	(new ChiselStage).emitVerilog(new wavegen.misc.ImageOutput, args)
	(new ChiselStage).emitVerilog(new Debouncer(5), args)
	(new ChiselStage).emitVerilog(new Debouncer(2), args)
}
