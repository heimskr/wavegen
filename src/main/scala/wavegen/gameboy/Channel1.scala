package wavegen.gameboy

import wavegen._
import chisel3._
import chisel3.util._

class Channel1(implicit clockFreq: Int) extends SquareChannel {
	val duty         = io.registers.NR11(7, 6)
	val lengthLoad   = io.registers.NR11(5, 0)
	val startVolume  = io.registers.NR12(7, 4)
	val addMode      = io.registers.NR12(3)
	val period       = io.registers.NR12(2, 0)
	val trigger      = io.registers.NR14(7)
	val lengthEnable = io.registers.NR14(6)
	val frequency    = Cat(io.registers.NR14(2, 0), io.registers.NR13)
	
	val sweeper = Module(new FrequencySweeper)
	sweeper.io.tick    := io.tick
	sweeper.io.trigger := trigger
	sweeper.io.period  := io.registers.NR10(6, 4)
	sweeper.io.negate  := io.registers.NR10(3)
	sweeper.io.shift   := io.registers.NR10(2, 0)

	val sweepClocker = Module(new Clocker)
	sweepClocker.io.enable := true.B
	sweepClocker.io.freq   := sweeper.io.out

	val envelope = Module(new Envelope)
	envelope.io.trigger := trigger
	envelope.io.initialVolume := startVolume
	envelope.io.rising := addMode
	envelope.io.period := period

	val clocker256 = Module(new Clocker)
	clocker256.io.enable := true.B
	clocker256.io.freq := 256.U

	val lengthCounter = Module(new LengthCounter)
	lengthCounter.io.tick      := clocker256.io.tick
	lengthCounter.io.trigger   := trigger
	lengthCounter.io.enable    := lengthEnable
	lengthCounter.io.loadValue := lengthLoad

	val waveforms = VecInit(Seq("b00000001".U, "b00000011".U, "b00001111".U, "b11111100".U))

	val squareGen = Module(new SquareGen(4, 8))
	squareGen.io.pause := !lengthCounter.io.channelOn
	squareGen.io.freq  := sweeper.io.out
	squareGen.io.max   := "b1111".U
	squareGen.io.wave  := waveforms(duty)

	io.out := 0.U
	
	when (lengthCounter.io.channelOn) {
		io.out := squareGen.io.out
	}
}
