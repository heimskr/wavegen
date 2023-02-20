package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

object GameBoy {
	val cpuFreq = 4_194_304
	// val cpuFreq = 25_000_000
	// val cpuFreq = 1_000_000

	val simulationFreq = 25_000_000
}

class GameBoy(implicit clockFreq: Int, inSimulator: Boolean) extends Module {
	val freq =
		if (inSimulator)
			GameBoy.simulationFreq
		else
			GameBoy.cpuFreq

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(8.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val out     = Output(UInt(7.W))
		val addr    = Output(UInt(18.W))
		val leds    = Output(UInt(8.W))
		val error   = Output(UInt(4.W))
	})

	val cpuClocker = Module(new StaticClocker(freq, clockFreq))
	// val slow = Module(new Clocker)
	val stateMachine = Module(new StateMachine(freq))
	val channel1 = Module(new Channel1(freq))

	// val freq = io.sw(7, 4) << 4.U

	cpuClocker.io.enable := true.B

	// slow.io.enable := true.B
	// slow.io.freq.bits  := freq
	// slow.io.freq.valid := true.B

	stateMachine.io.start := io.start
	// stateMachine.io.start := io.buttonC
	stateMachine.io.tick  := cpuClocker.io.tick
	stateMachine.io.rom   := io.rom
	// stateMachine.io.pause := !(slow.io.period.valid && channel1.io.out.valid)
	stateMachine.io.pause := !channel1.io.out.valid

	io.addr  := stateMachine.io.addr
	io.error := stateMachine.io.error
	io.leds  := 0.U

	channel1.io.tick := cpuClocker.io.tick
	channel1.io.registers := stateMachine.io.registers

	switch (io.sw(3, 0)) {
		is ( 0.U) { io.leds := stateMachine.io.errorInfo2(7, 0) }
		is ( 1.U) { io.leds := stateMachine.io.errorInfo2(15, 8) }
		is ( 2.U) { io.leds := stateMachine.io.errorInfo }
		// is ( 3.U) { io.leds := Cat(io.start, stateMachine.io.pause, slow.io.period.valid, channel1.io.out.valid, stateMachine.io.state) }
		is ( 3.U) { io.leds := Cat(io.start, stateMachine.io.pause, 0.U(2.W), stateMachine.io.state) }
		is ( 4.U) { io.leds := Cat(io.start, reset.asBool, 0.U(2.W), stateMachine.io.error) }
		is ( 5.U) { io.leds := stateMachine.io.addr(17, 16) }
		is ( 6.U) { io.leds := stateMachine.io.addr( 7,  0) }
		is ( 7.U) { io.leds := stateMachine.io.addr(15,  8) }
		is ( 8.U) { io.leds := "b10101010".U }
		is ( 9.U) { io.leds := Fill(8, cpuClocker.io.tick) }
		// is (10.U) { io.leds := Fill(8,    slow.io.tick) }
		is (10.U) { io.leds := stateMachine.io.errorInfo3 }
		// is (11.U) { io.leds := slow.io.period( 7,  0) }
		// is (12.U) { io.leds := slow.io.period(15,  8) }
		// is (13.U) { io.leds := slow.io.counter( 7,  0) }
		// is (14.U) { io.leds := slow.io.counter(15,  8) }
		// is (15.U) { io.leds := slow.io.counter(23, 16) }
		is (11.U) { io.leds := Cat(channel1.io.out.valid, 0.U(3.W), channel1.io.out.bits) }
		is (12.U) { io.leds := stateMachine.io.info }
		is (13.U) { io.leds := Cat(io.buttonU, io.buttonR, io.buttonD, io.buttonL, io.buttonC) }
		// is (14.U) { io.leds := Cat(stateMachine.io.info(5, 0), slow.io.period.valid, channel1.io.out.valid) }
		is (14.U) { io.leds := Cat(stateMachine.io.info(6, 0), channel1.io.out.valid) }
		is (15.U) { io.leds := Cat(io.rom) }
	}

	io.out := channel1.io.out.bits
}
