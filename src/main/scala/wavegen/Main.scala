package wavegen

import chisel3._
import chisel3.util._
import chisel3.stage._

class MainGB extends Module {
	implicit val clockFreq   = 100_000_000
	implicit val inSimulator = false
	val addressWidth = 18
	val romWidth = 24

	val io = IO(new Bundle {
		val cpuClock = Input(Bool())
		val pulseU   = Input(Bool())
		val pulseR   = Input(Bool())
		val pulseD   = Input(Bool())
		val pulseL   = Input(Bool())
		val pulseC   = Input(Bool())
		val sw       = Input(UInt(8.W))
		val rom      = Input(UInt(romWidth.W))
		val outL     = Output(UInt(24.W))
		val outR     = Output(UInt(24.W))
		val led      = Output(UInt(8.W))
		val addr     = Output(UInt(addressWidth.W))
	})

	var centerReg = RegInit(false.B)

	val gameboy = Module(new wavegen.gameboy.GameBoy(addressWidth, romWidth))
	gameboy.io.tick  := io.cpuClock
	gameboy.io.start := io.pulseC
	gameboy.io.sw    := io.sw

	def increase9to24(value: UInt): UInt = value << 9.U

	val multiplierWidth = 4
	val multiplier = RegInit(4.U(multiplierWidth.W))
	val lastU = RegInit(false.B)
	val lastD = RegInit(false.B)

	when (io.pulseU && multiplier =/= ((1 << multiplierWidth) - 1).U) {
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

class MainNES extends Module {
	implicit val clockFreq   = 100_000_000
	implicit val inSimulator = false
	val addressWidth = 17
	val romWidth = 24

	val io = IO(new Bundle {
		val cpuClock = Input(Bool())
		val pulseU   = Input(Bool())
		val pulseR   = Input(Bool())
		val pulseD   = Input(Bool())
		val pulseL   = Input(Bool())
		val pulseC   = Input(Bool())
		val sw       = Input(UInt(8.W))
		val rom      = Input(UInt(romWidth.W))
		val outL     = Output(UInt(24.W))
		val outR     = Output(UInt(24.W))
		val led      = Output(UInt(8.W))
		val addr     = Output(UInt(addressWidth.W))
	})

	var centerReg = RegInit(false.B)

	val nes = Module(new wavegen.nes.NES(addressWidth, romWidth))
	nes.io.tick  := io.cpuClock
	nes.io.start := io.pulseC
	nes.io.sw    := io.sw

	def increase10to24(value: UInt): UInt = value << 9.U

	val multiplierWidth = 4
	val multiplier = RegInit(4.U(multiplierWidth.W))
	val lastU = RegInit(false.B)
	val lastD = RegInit(false.B)

	when (io.pulseU && multiplier =/= ((1 << multiplierWidth) - 1).U) {
		multiplier := multiplier + 1.U
	}

	when (io.pulseD && multiplier =/= 0.U) {
		multiplier := multiplier - 1.U
	}

	io.outL := increase10to24(nes.io.outL) * multiplier
	io.outR := increase10to24(nes.io.outR) * multiplier
	io.led  := nes.io.leds
	io.addr := nes.io.addr
	nes.io.rom    := io.rom
	nes.io.pulseD := io.pulseD
	nes.io.pulseU := io.pulseU
	nes.io.pulseL := io.pulseL
	nes.io.pulseR := io.pulseR
	nes.io.pulseC := io.pulseC
}

class MainBoth extends Module {
	implicit val clockFreq   = 100_000_000
	implicit val inSimulator = false
	val romWidth = 24

	val io = IO(new Bundle {
		val clockGB  = Input(Bool())
		val clockNES = Input(Bool())
		val pulseU   = Input(Bool())
		val pulseR   = Input(Bool())
		val pulseD   = Input(Bool())
		val pulseL   = Input(Bool())
		val pulseC   = Input(Bool())
		val sw       = Input(UInt(8.W))
		val romGB    = Input(UInt(romWidth.W))
		val romNES   = Input(UInt(romWidth.W))
		val outL     = Output(UInt(24.W))
		val outR     = Output(UInt(24.W))
		val led      = Output(UInt(8.W))
		val addrGB   = Output(UInt(18.W))
		val addrNES  = Output(UInt(17.W))
	})

	val useNES = io.sw(0)

	var centerReg = RegInit(false.B)

	val gameboy = withReset(reset.asBool || useNES) { Module(new wavegen.gameboy.GameBoy(18, romWidth)) }
	gameboy.io.tick  := io.clockGB
	gameboy.io.start := io.pulseC && !useNES
	gameboy.io.sw    := io.sw

	val nes = withReset(reset.asBool || !useNES) { Module(new wavegen.nes.NES(17, romWidth)) }
	nes.io.tick  := io.clockNES
	nes.io.start := io.pulseC && useNES
	nes.io.sw    := io.sw

	def boost(value: UInt): UInt = value << 9.U

	val multiplierWidth = 4
	val multiplier = RegInit(4.U(multiplierWidth.W))
	val lastU = RegInit(false.B)
	val lastD = RegInit(false.B)

	when (io.pulseU && multiplier =/= ((1 << multiplierWidth) - 1).U) {
		multiplier := multiplier + 1.U
	}

	when (io.pulseD && multiplier =/= 0.U) {
		multiplier := multiplier - 1.U
	}

	nes.io.rom    := DontCare
	nes.io.pulseD := DontCare
	nes.io.pulseU := DontCare
	nes.io.pulseL := DontCare
	nes.io.pulseR := DontCare
	nes.io.pulseC := DontCare
	gameboy.io.rom    := DontCare
	gameboy.io.pulseD := DontCare
	gameboy.io.pulseU := DontCare
	gameboy.io.pulseL := DontCare
	gameboy.io.pulseR := DontCare
	gameboy.io.pulseC := DontCare
	io.addrGB  := gameboy.io.addr
	io.addrNES := nes.io.addr

	when (useNES) {
		io.outL := boost(nes.io.outL) * multiplier
		io.outR := boost(nes.io.outR) * multiplier
		io.led  := nes.io.leds
		nes.io.rom    := io.romNES
		nes.io.pulseD := io.pulseD
		nes.io.pulseU := io.pulseU
		nes.io.pulseL := io.pulseL
		nes.io.pulseR := io.pulseR
		nes.io.pulseC := io.pulseC
	} .otherwise {
		io.outL := boost(gameboy.io.outL) * multiplier
		io.outR := boost(gameboy.io.outR) * multiplier
		io.led  := gameboy.io.leds
		gameboy.io.rom    := io.romGB
		gameboy.io.pulseD := io.pulseD
		gameboy.io.pulseU := io.pulseU
		gameboy.io.pulseL := io.pulseL
		gameboy.io.pulseR := io.pulseR
		gameboy.io.pulseC := io.pulseC
	}
}

object MainRun extends scala.App {
	(new ChiselStage).emitVerilog(new MainBoth, args)
	(new ChiselStage).emitVerilog(new wavegen.misc.ImageOutput, args)
	(new ChiselStage).emitVerilog(new Debouncer(5), args)
	(new ChiselStage).emitVerilog(new Debouncer(2), args)
}
