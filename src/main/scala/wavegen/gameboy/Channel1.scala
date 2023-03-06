package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel1 extends Module {
	override val desiredName = "GBChannel1"

	val io = IO(new ChannelIO {
		val sweeperTick  = Input(Bool())
		val envelopeTick = Input(Bool())
		val lengthTick   = Input(Bool())
		val nr13         = Valid(UInt(8.W))
		val nr14         = Valid(UInt(8.W))
		val freq         = Output(UInt(11.W))
	})

	val duty         = io.registers.NR11(7, 6)
	val lengthLoad   = io.registers.NR11(5, 0)
	val startVolume  = io.registers.NR12(7, 4)
	val addMode      = io.registers.NR12(3)
	val period       = io.registers.NR12(2, 0)
	val trigger      = io.registers.NR14(7)
	val lengthEnable = io.registers.NR14(6)
	val frequency    = Cat(io.registers.NR14(2, 0), io.registers.NR13)

	val sweeper = Module(new FrequencySweeper)
	sweeper.io.tick        := io.sweeperTick
	sweeper.io.trigger     := trigger
	sweeper.io.period      := io.registers.NR10(6, 4)
	sweeper.io.negate      := io.registers.NR10(3)
	sweeper.io.shift       := io.registers.NR10(2, 0)
	sweeper.io.frequencyIn := frequency
	sweeper.io.nr14In      := io.registers.NR14
	io.nr13 <> sweeper.io.nr13Out
	io.nr14 <> sweeper.io.nr14Out

	val envelope = Module(new Envelope)
	envelope.io.tick          := io.envelopeTick
	envelope.io.trigger       := trigger
	envelope.io.initialVolume := startVolume
	envelope.io.rising        := addMode
	envelope.io.period        := period

	val lengthCounter = Module(new LengthCounter)
	lengthCounter.io.tick      := io.lengthTick
	lengthCounter.io.trigger   := trigger
	lengthCounter.io.enable    := lengthEnable
	lengthCounter.io.loadValue := lengthLoad

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))

	val latestFrequency = WireInit(frequency)
	when (sweeper.io.nr13Out.valid && sweeper.io.nr14Out.valid) {
		latestFrequency := Cat(sweeper.io.nr14Out.bits(2, 0), sweeper.io.nr13Out.bits)
	}

	val frequencyReg = RegInit(0.U(11.W))
	frequencyReg := latestFrequency

	val sweepClocker = Module(new StoredPeriodClocker(14))
	sweepClocker.io.tickIn       := io.tick
	sweepClocker.io.period.bits  := (2048.U - frequencyReg) << 2.U
	sweepClocker.io.period.valid := true.B

	val squareGen = Module(new SquareGenExternal(1, 8))
	squareGen.io.tick := sweepClocker.io.tickOut
	squareGen.io.max  := "b1".U
	squareGen.io.wave := waveforms(duty)

	io.out  := 0.U
	io.freq := latestFrequency

	when (lengthCounter.io.channelOn) {
		io.out := Mux(squareGen.io.out(0), envelope.io.currentVolume, 0.U)
	}
}
