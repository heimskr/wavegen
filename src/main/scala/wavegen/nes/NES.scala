package wavegen.nes

import wavegen._
import chisel3._
import chisel3.util._

object NES {
	val cpuFreq = 1_789_772
	val simulationFreq = 25_000_000
}

class NES(addressWidth: Int, romWidth: Int, useInternalClock: Boolean = true)(implicit clockFreq: Int, inSimulator: Boolean) extends Module {
	val slowFreq = if (inSimulator) NES.simulationFreq else NES.cpuFreq

	val io = IO(new Bundle {
		val tick   = Input(Bool())
		val start  = Input(Bool())
		val rom    = Input(UInt(romWidth.W))
		val sw     = Input(UInt(8.W))
		val pulseU = Input(Bool())
		val pulseR = Input(Bool())
		val pulseD = Input(Bool())
		val pulseL = Input(Bool())
		val pulseC = Input(Bool())
		val outL   = Output(UInt(10.W))
		val outR   = Output(UInt(10.W))
		val addr   = Output(UInt(addressWidth.W))
		val leds   = Output(UInt(8.W))
	})

	val frameCounter = Module(new FrameCounter)
	val stateMachine = Module(new NESStateMachine(addressWidth, romWidth))
	val channel1     = Module(new PulseChannel(1))
	val channel2     = Module(new PulseChannel(2))
	// val channel3     = Module(new Channel3)
	// val channel4     = Module(new Channel4)
	// val channel5     = Module(new Channel5)

	val cpuTick = if (useInternalClock) {
		val cpuClocker = Module(new StaticClocker(slowFreq, clockFreq, false, "NESCPUClocker"))
		cpuClocker.io.enable := true.B
		cpuClocker.io.tick
	} else io.tick

	stateMachine.io.start := io.pulseC
	stateMachine.io.tick  := cpuTick
	stateMachine.io.rom   := io.rom

	frameCounter.io.reload   := stateMachine.io.reloadFC
	frameCounter.io.cpuTick  := cpuTick
	frameCounter.io.register := stateMachine.io.registers.$4017

	val ticks = frameCounter.io.ticks

	channel1.io.ticks     := ticks
	channel1.io.registers := stateMachine.io.registers
	channel1.io.reg0      := stateMachine.io.registers.$4000
	channel1.io.reg1      := stateMachine.io.registers.$4001
	channel1.io.reg2      := stateMachine.io.registers.$4002
	channel1.io.reg3      := stateMachine.io.registers.$4003
	channel1.io.writes    := stateMachine.io.pulse1Writes

	channel2.io.ticks     := ticks
	channel2.io.registers := stateMachine.io.registers
	channel2.io.reg0      := stateMachine.io.registers.$4004
	channel2.io.reg1      := stateMachine.io.registers.$4005
	channel2.io.reg2      := stateMachine.io.registers.$4006
	channel2.io.reg3      := stateMachine.io.registers.$4007
	channel2.io.writes    := stateMachine.io.pulse2Writes

	val sum = channel1.io.out + channel2.io.out

	io.leds := stateMachine.io.state
	io.outL := sum
	io.outR := sum
	io.addr := stateMachine.io.addr
}
