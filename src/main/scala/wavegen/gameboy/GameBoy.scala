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

class GameBoy(addressWidth: Int, romWidth: Int)(implicit clockFreq: Int, inSimulator: Boolean) extends Module {
	val slowFreq = if (inSimulator) GameBoy.simulationFreq else GameBoy.cpuFreq
	val fsFreq = if (inSimulator) 2048 else slowFreq

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(romWidth.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val outL    = Output(UInt(7.W))
		val outR    = Output(UInt(7.W))
		val addr    = Output(UInt(addressWidth.W))
		val leds    = Output(UInt(8.W))
		val error   = Output(UInt(4.W))
	})

	val cpuClocker = Module(new StaticClocker(slowFreq, clockFreq))
	val stateMachine = Module(new StateMachine(addressWidth, romWidth))
	val channel1 = Module(new Channel1(slowFreq, fsFreq))

	// val freq = io.sw(7, 4) << 4.U

	cpuClocker.io.enable := true.B

	// slow.io.enable := true.B
	// slow.io.freq.bits  := freq
	// slow.io.freq.valid := true.B

	stateMachine.io.start := io.start
	// stateMachine.io.start := io.buttonC
	stateMachine.io.rom   := io.rom
	// stateMachine.io.pause := !(slow.io.period.valid && channel1.io.out.valid)
	stateMachine.io.pause := !channel1.io.out.valid
	stateMachine.io.nr13In <> channel1.io.nr13
	stateMachine.io.nr14In <> channel1.io.nr14

	val registers = stateMachine.io.registers

	io.addr  := stateMachine.io.addr
	io.error := stateMachine.io.error
	io.leds  := 0.U

	channel1.io.tick := cpuClocker.io.tick
	channel1.io.registers := stateMachine.io.registers
	channel1.io.buttonD := io.buttonD
	channel1.io.buttonR := io.buttonR

	switch (io.sw(4, 0)) {
		is ( 0.U) { io.leds := stateMachine.io.errorInfo2(7, 0) }
		is ( 1.U) { io.leds := stateMachine.io.errorInfo2(15, 8) }
		is ( 2.U) { io.leds := stateMachine.io.errorInfo }
		// is ( 3.U) { io.leds := Cat(io.start, stateMachine.io.pause, slow.io.period.valid, channel1.io.out.valid, stateMachine.io.state) }
		is ( 3.U) { io.leds := Cat(io.start, stateMachine.io.pause, 0.U(2.W), stateMachine.io.state) }
		is ( 4.U) { io.leds := Cat(io.start, reset.asBool, 0.U(2.W), stateMachine.io.error) }
		is ( 5.U) { io.leds := stateMachine.io.addr(16) }
		is ( 6.U) { io.leds := stateMachine.io.addr( 7, 0) }
		is ( 7.U) { io.leds := stateMachine.io.addr(15, 8) }
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
		// is (14.U) { io.leds := io.rom }
		is (15.U) { io.leds := stateMachine.io.adjusted }
		is (16.U) { io.leds := stateMachine.io.value }
		is (17.U) { io.leds := stateMachine.io.pointer(7, 0) }
		is (18.U) { io.leds := stateMachine.io.pointer(15, 8) }
		is (19.U) { io.leds := stateMachine.io.opcode }
		is (20.U) { io.leds := stateMachine.io.waitCounter( 7,  0) }
		is (21.U) { io.leds := stateMachine.io.waitCounter(15,  8) }
		is (22.U) { io.leds := stateMachine.io.waitCounter(23, 16) }
		is (23.U) { io.leds := stateMachine.io.waitCounter(31, 24) }
		is (24.U) { io.leds := io.rom(7, 0) }
		is (25.U) { io.leds := io.rom(15, 8) }
		is (26.U) { io.leds := io.rom(23, 16) }
		is (27.U) { io.leds := stateMachine.io.operand1 }
		is (28.U) { io.leds := stateMachine.io.operand2 }
		is (29.U) {  }
		is (30.U) { io.leds := channel1.io.freq(7, 0) }
		is (31.U) { io.leds := channel1.io.freq(10, 8) }
	}

	val channels = VecInit(0.U(4.W), 0.U(4.W), 0.U(4.W), channel1.io.out.bits)

	// Some silliness to account for channel enable/disable in NR51 and panning in NR50
	Seq(io.outR, io.outL).zipWithIndex.foreach { case (out, i) =>
		val to_mult = (channels.zipWithIndex.map { case (channel, j) =>
			Mux(registers.NR51(3 + 4 * i - j), channel, 0.U(4.W))
		}.foldLeft(0.U)(_ +& _))(7, 0)
		out := (registers.NR50(2 + 4 * i, 4 * i) * to_mult +& to_mult) >> 3.U
	}
}
