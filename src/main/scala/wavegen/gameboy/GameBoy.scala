package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

object GameBoy {
	val cpuFreq = 4_194_304
	val simulationFreq = 25_000_000
}

class GameBoy(addressWidth: Int, romWidth: Int)(implicit clockFreq: Int, inSimulator: Boolean) extends Module {
	val slowFreq = if (inSimulator) GameBoy.simulationFreq else GameBoy.cpuFreq
	val fsFreq = if (inSimulator) 2048 else GameBoy.cpuFreq

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(romWidth.W))
		val sw      = Input(UInt(8.W))
		val buttonU = Input(Bool())
		val buttonR = Input(Bool())
		val buttonD = Input(Bool())
		val buttonL = Input(Bool())
		val buttonC = Input(Bool())
		val outL    = Output(UInt(8.W))
		val outR    = Output(UInt(8.W))
		val addr    = Output(UInt(addressWidth.W))
		val leds    = Output(UInt(8.W))
		val error   = Output(UInt(4.W))
	})

	val cpuClocker   = Module(new StaticClocker(slowFreq, clockFreq))
	val stateMachine = Module(new StateMachine(addressWidth, romWidth))
	val channel1     = Module(new Channel1)
	val channel2     = Module(new Channel2)
	val channel4     = Module(new Channel4)
	val sequencer    = Module(new FrameSequencer(fsFreq))

	cpuClocker.io.enable := !io.sw(7) ^ io.buttonD
	val cpuTick = cpuClocker.io.tick

	sequencer.io.tick := cpuTick

	stateMachine.io.start := io.start
	stateMachine.io.rom   := io.rom
	stateMachine.io.tick  := cpuTick
	stateMachine.io.nr13In <> channel1.io.nr13
	stateMachine.io.nr14In <> channel1.io.nr14

	val registers = stateMachine.io.registers

	io.addr  := stateMachine.io.addr
	io.error := stateMachine.io.error
	io.leds  := 0.U

	channel1.io.tick         := cpuTick
	channel1.io.sweeperTick  := sequencer.io.sweeper
	channel1.io.envelopeTick := sequencer.io.envelope
	channel1.io.lengthTick   := sequencer.io.lengthCounter
	channel1.io.registers    := stateMachine.io.registers
	channel1.io.buttonD      := io.buttonD
	channel1.io.buttonR      := io.buttonR

	channel2.io.tick         := cpuTick
	channel2.io.envelopeTick := sequencer.io.envelope
	channel2.io.lengthTick   := sequencer.io.lengthCounter
	channel2.io.registers    := stateMachine.io.registers
	channel2.io.buttonD      := io.buttonD
	channel2.io.buttonR      := io.buttonR

	channel4.io.tick         := cpuTick
	channel4.io.registers    := stateMachine.io.registers
	channel4.io.envelopeTick := sequencer.io.envelope
	channel4.io.lengthTick   := sequencer.io.lengthCounter

	switch (io.sw(4, 0)) {
		is ( 0.U) { io.leds := stateMachine.io.errorInfo2(7, 0) }
		is ( 1.U) { io.leds := stateMachine.io.errorInfo2(15, 8) }
		is ( 2.U) { io.leds := stateMachine.io.errorInfo }
		is ( 3.U) { io.leds := Cat(io.start, stateMachine.io.tick, 0.U(2.W), stateMachine.io.state) }
		is ( 4.U) { io.leds := Cat(io.start, reset.asBool, 0.U(2.W), stateMachine.io.error) }
		is ( 5.U) { io.leds := stateMachine.io.addr(16) }
		is ( 6.U) { io.leds := stateMachine.io.addr( 7, 0) }
		is ( 7.U) { io.leds := stateMachine.io.addr(15, 8) }
		is ( 8.U) { io.leds := "b10101010".U }
		is ( 9.U) { io.leds := Fill(8, cpuTick) }
		is (10.U) { io.leds := stateMachine.io.errorInfo3 }
		is (11.U) { io.leds := Cat(0.U(4.W), channel1.io.out) }
		is (12.U) { io.leds := stateMachine.io.info }
		is (13.U) { io.leds := Cat(io.buttonU, io.buttonR, io.buttonD, io.buttonL, io.buttonC) }
		is (14.U) { io.leds := Cat(channel4.io.channelOn, 0.U(3.W), channel4.io.currentVolume) }
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
		is (29.U) { io.leds := Cat(0.U(4.W), channel2.io.out) }
		is (30.U) { io.leds := channel1.io.freq(7, 0) }
		is (31.U) { io.leds := channel1.io.freq(10, 8) }
	}

	val channels = VecInit(channel1.io.out, channel2.io.out, 0.U(4.W), channel4.io.out)
	val storedChannels = RegInit(VecInit(0.U(4.W), 0.U(4.W), 0.U(4.W), 0.U(4.W)))

	val mixer = Module(new ChannelMixer)
	mixer.io.in.valid := !(channels === storedChannels)
	mixer.io.in.bits  := channels
	mixer.io.nr50     := registers.NR50
	mixer.io.nr51     := registers.NR51

	when (mixer.io.out.valid) {
		io.outL := mixer.io.out.bits.left
		io.outR := mixer.io.out.bits.right
	} .otherwise {
		io.outL := 0.U
		io.outR := 0.U
	}

	storedChannels := channels
}
