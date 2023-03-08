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
	val useInternalClocks = true

	val io = IO(new Bundle {
		val clockGB    = Input(Bool())
		val clockNES   = Input(Bool())
		val pulseU     = Input(Bool())
		val pulseR     = Input(Bool())
		val pulseD     = Input(Bool())
		val pulseL     = Input(Bool())
		val pulseC     = Input(Bool())
		val sw         = Input(UInt(8.W))
		val romGB      = Input(UInt(romWidth.W))
		val romNES     = Input(UInt(romWidth.W))
		val outL       = Output(UInt(24.W))
		val outR       = Output(UInt(24.W))
		val led        = Output(UInt(8.W))
		val addrGB     = Output(UInt(18.W))
		val addrNES    = Output(UInt(17.W))
		val jaIn       = Input(UInt(8.W))
		val pulseOut   = Output(Bool())
		val latchOut   = Output(Bool())
		val rxByte     = Flipped(Valid(UInt(8.W)))
		val txByte     = Valid(UInt(8.W))
		val nesButtons = Output(NESButtons())
		val useNES     = Output(Bool())
	})

	io.addrGB := DontCare
	io.addrNES := DontCare
	io.outL := 0.U
	io.outR := 0.U
	io.led  := 0.U
	io.txByte.bits  := 0.U
	io.txByte.valid := false.B

	val nesA      = RegInit(false.B)
	val nesB      = RegInit(false.B)
	val nesSelect = RegInit(false.B)
	val nesStart  = RegInit(false.B)
	val nesUp     = RegInit(false.B)
	val nesDown   = RegInit(false.B)
	val nesLeft   = RegInit(false.B)
	val nesRight  = RegInit(false.B)


	val nesDebouncer = Module(new Debouncer(8))
	nesDebouncer.io.in   := VecInit(nesA, nesB, nesSelect, nesStart, nesUp, nesDown, nesLeft, nesRight)
	io.nesButtons.a      := nesDebouncer.io.out(0)
	io.nesButtons.b      := nesDebouncer.io.out(1)
	io.nesButtons.select := nesDebouncer.io.out(2)
	io.nesButtons.start  := nesDebouncer.io.out(3)
	io.nesButtons.up     := nesDebouncer.io.out(4)
	io.nesButtons.down   := nesDebouncer.io.out(5)
	io.nesButtons.left   := nesDebouncer.io.out(6)
	io.nesButtons.right  := nesDebouncer.io.out(7)

	val useNES = RegInit(false.B)
	io.useNES := useNES
	when (io.nesButtons.select) {
		useNES := !useNES
	}

	val start = io.pulseC || io.nesButtons.start

	val gameboy = withReset(reset.asBool || useNES) { Module(new wavegen.gameboy.GameBoy(18, romWidth, useInternalClocks)) }
	gameboy.io.tick  := io.clockGB
	gameboy.io.start := start && !useNES
	gameboy.io.sw    := io.sw

	val nes = withReset(reset.asBool || !useNES) { Module(new wavegen.nes.NES(17, romWidth, useInternalClocks)) }
	nes.io.tick  := io.clockNES
	nes.io.start := start && useNES
	nes.io.sw    := io.sw

	def boost(value: UInt): UInt = value << 9.U

	val multiplierWidth = 4
	val multiplier = RegInit(4.U(multiplierWidth.W))
	val lastU = RegInit(false.B)
	val lastD = RegInit(false.B)

	when ((io.pulseU || io.nesButtons.up) && multiplier =/= ((1 << multiplierWidth) - 1).U) {
		multiplier := multiplier + 1.U
	}

	when ((io.pulseD || io.nesButtons.down) && multiplier =/= 0.U) {
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

	val reset12us = RegInit(true.B)
	val reset6us  = RegInit(true.B)

	val clock60 = Module(new StaticClocker(60, clockFreq, true))
	clock60.io.enable := true.B

	val clock12us = withReset(reset.asBool || reset12us) { Module(new PeriodClocker(20)) }
	clock12us.io.tickIn := true.B
	clock12us.io.period := 1200.U

	val clock6us = withReset(reset.asBool || reset6us) { Module(new PeriodClocker(20)) }
	clock6us.io.tickIn := true.B
	clock6us.io.period := 600.U

	val jaPulseOut = RegInit(false.B)
	val jaLatchOut = RegInit(false.B)
	val jaDataIn   = io.jaIn(1)

	val sWait60 :: sInitial12 :: sWait6 :: s8PulsesOn :: s8PulsesOff :: Nil = Enum(5)
	val state    = RegInit(sWait60)
	val counter8 = RegInit(0.U(3.W))

	when (state === sWait60) {
		reset6us  := true.B
		reset12us := true.B
		when (clock60.io.tick) {
			state := sInitial12
			reset6us   := false.B
			reset12us  := false.B
			jaLatchOut := true.B
		}
	} .elsewhen (state === sInitial12) {
		when (clock12us.io.tickOut) {
			jaLatchOut := false.B
			state      := sWait6
			nesA       := !jaDataIn
		}
	} .elsewhen (state === sWait6) {
		when (clock6us.io.tickOut) {
			counter8  := 0.U
			state     := s8PulsesOn
		}
	} .elsewhen (state === s8PulsesOn) {
		jaPulseOut := true.B

		when (clock6us.io.tickOut) {
			jaPulseOut := false.B
			state      := s8PulsesOff

			switch (counter8) {
				is (0.U) { nesB      := !jaDataIn }
				is (1.U) { nesSelect := !jaDataIn }
				is (2.U) { nesStart  := !jaDataIn }
				is (3.U) { nesUp     := !jaDataIn }
				is (4.U) { nesDown   := !jaDataIn }
				is (5.U) { nesLeft   := !jaDataIn }
				is (6.U) { nesRight  := !jaDataIn }
			}

			when (counter8 === 7.U) {
				counter8 := 0.U
				state    := sWait60
			} .otherwise {
				counter8 := counter8 + 1.U
			}
		}
	} .elsewhen (state === s8PulsesOff) {
		jaPulseOut := false.B
		when (clock6us.io.tickOut) {
			state := s8PulsesOn
		}
	}

	io.latchOut := jaLatchOut
	io.pulseOut := jaPulseOut
}

object MainRun extends scala.App {
	(new ChiselStage).emitVerilog(new MainBoth, args)
	(new ChiselStage).emitVerilog(new wavegen.misc.ImageOutput, args)
	(new ChiselStage).emitVerilog(new Debouncer(5), args)
	(new ChiselStage).emitVerilog(new Debouncer(2), args)
}
