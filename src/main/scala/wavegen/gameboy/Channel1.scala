package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel1(freq256: Int = 256)(implicit clockFreq: Int) extends Module {
	val io = IO(new SquareChannelIO {
		val info = Output(UInt(2.W))
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
	sweeper.io.tick        := io.tick
	sweeper.io.trigger     := trigger
	sweeper.io.period      := io.registers.NR10(6, 4)
	sweeper.io.negate      := io.registers.NR10(3)
	sweeper.io.shift       := io.registers.NR10(2, 0)
	sweeper.io.frequencyIn := frequency

	val sweepClocker = Module(new Clocker)
	sweepClocker.io.enable := true.B
	sweepClocker.io.freq.bits  := sweeper.io.out
	sweepClocker.io.freq.valid := true.B

	val envelope = Module(new Envelope)
	envelope.io.trigger := trigger
	envelope.io.initialVolume := startVolume
	envelope.io.rising := addMode
	envelope.io.period := period

	val clocker256 = Module(new Clocker)
	clocker256.io.enable := true.B
	clocker256.io.freq.bits  := freq256.U
	clocker256.io.freq.valid := true.B

	val lengthCounter = Module(new LengthCounter)
	lengthCounter.io.tick      := clocker256.io.tick
	lengthCounter.io.trigger   := trigger
	lengthCounter.io.enable    := lengthEnable
	lengthCounter.io.loadValue := lengthLoad

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))

	// val squareGen = Module(new SquareGen(1, 8))
	// squareGen.io.pause := !lengthCounter.io.channelOn
	// squareGen.io.freq  := sweeper.io.out
	// squareGen.io.max   := "b1".U
	// squareGen.io.wave  := waveforms(duty)

	val squareGen = Module(new SquareGenExternal(1, 8))
	squareGen.io.tick  := sweepClocker.io.tick
	squareGen.io.max   := "b1".U
	squareGen.io.wave  := waveforms(duty)

	io.out.bits  := 0.U
	io.out.valid := sweepClocker.io.period.valid && clocker256.io.period.valid
	io.info := Cat(sweepClocker.io.period.valid, clocker256.io.period.valid)

	when (lengthCounter.io.channelOn) {
		io.out.bits := squareGen.io.out(0) * envelope.io.currentVolume
	}
}
