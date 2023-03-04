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
	val fsFreq   = if (inSimulator) 2048 else GameBoy.cpuFreq

	val io = IO(new Bundle {
		val start   = Input(Bool())
		val rom     = Input(UInt(romWidth.W))
		val sw      = Input(UInt(8.W))
		val pulseU  = Input(Bool())
		val pulseR  = Input(Bool())
		val pulseD  = Input(Bool())
		val pulseL  = Input(Bool())
		val pulseC  = Input(Bool())
		val outL    = Output(UInt(9.W))
		val outR    = Output(UInt(9.W))
		val addr    = Output(UInt(addressWidth.W))
		val leds    = Output(UInt(8.W))
		val error   = Output(UInt(4.W))
	})

	val cpuClocker   = Module(new StaticClocker(slowFreq, clockFreq))
	val stateMachine = Module(new GBStateMachine(addressWidth, romWidth))
	val channel1     = Module(new Channel1)
	val channel2     = Module(new Channel2)
	val channel3     = Module(new Channel3(true))
	val channel4     = Module(new Channel4)
	val sequencer    = Module(new FrameSequencer(fsFreq))

	// cpuClocker.io.enable := !io.sw(0) ^ io.pulseD
	cpuClocker.io.enable := true.B
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

	channel2.io.tick         := cpuTick
	channel2.io.envelopeTick := sequencer.io.envelope
	channel2.io.lengthTick   := sequencer.io.lengthCounter
	channel2.io.registers    := stateMachine.io.registers

	channel3.io.tick         := cpuTick
	channel3.io.lengthTick   := sequencer.io.lengthCounter
	channel3.io.registers    := stateMachine.io.registers

	channel4.io.tick         := cpuTick
	channel4.io.registers    := stateMachine.io.registers
	channel4.io.envelopeTick := sequencer.io.envelope
	channel4.io.lengthTick   := sequencer.io.lengthCounter

	val channel1Stored = Reg(UInt(4.W))
	val channel2Stored = Reg(UInt(4.W))
	val channel3Stored = Reg(UInt(4.W))
	val channel4Stored = Reg(UInt(4.W))
	channel1Stored := channel1.io.out
	channel2Stored := channel2.io.out
	channel3Stored := channel3.io.out
	channel4Stored := channel4.io.out

	// io.leds := io.sw
	io.leds := stateMachine.io.state

	val channels    = VecInit(channel1Stored, channel2Stored, channel3Stored, channel4Stored)
	val oldChannels = RegInit(VecInit(0.U(4.W), 0.U(4.W), 0.U(4.W), 0.U(4.W)))

	val mixer = Module(new ChannelMixer(4))
	mixer.io.in.valid := !(channels === oldChannels)
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

	oldChannels := channels
}
