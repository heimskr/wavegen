package wavegen

import chisel3._
import chisel3.util._
import scala.collection.immutable.ListMap

import wavegen.Maxer
class Mixer(channelCount: Int, width: Int, memorySize: Int) extends Module {
	val sInit :: sSumming :: Nil = Enum(2)

	val io = IO(new Bundle {
		val in  = Input(Vec(channelCount, Flipped(Decoupled(UInt(width.W)))))
		val out = Decoupled(UInt(width.W))
	})

	// var inputs = Seq.tabulate(channelCount)(n => ("channel" + n) -> Flipped(Decoupled(UInt(width.W))))
	// var channelKeys = inputs.map(_._1)
	// val io = IO(new CustomBundle(inputs :+ ("out" -> Decoupled(UInt(width.W)))))
	// val out = io("out").asInstanceOf[DecoupledIO[UInt]]

	// def apply(channel: String) = io(channel).asInstanceOf[DecoupledIO[UInt]]

	val bigger = width + log2Ceil(channelCount)
	val maxReg = RegInit(0.U(bigger.W))

	// val allValid = channelKeys.foldLeft(true.B)(_ && io(_).asInstanceOf[DecoupledIO[UInt]].valid)
	
	val state = RegInit(sInit)

	io.out.valid := false.B
	io.out.bits := 0.U

	// channelKeys.foreach { this(_).ready := false.B }
	
	io.in.foreach { in => in.ready := true.B }

	// when (state === init) {
	// 	out.valid := allValid
	// }

	val maxer = Module(new Maxer(channelCount + 1, bigger))
	val summer = Module(new Summer(channelCount, width))

	for (i <- 0 until channelCount) {
		maxer.io.in(i)  := io.in(i).bits
		summer.io.in(i) := io.in(i).bits
	}

	maxer.io.in(channelCount) := maxReg

	val allValid = io.in.foldLeft(true.B)(_ && _.valid)

	val memory = Reg(Vec(memorySize, UInt(bigger.W)))
	val index = RegInit(0.U(log2Ceil(memorySize).W))

	

	when (state === sInit) {
		when (allValid) {
			// maxReg := maxer.io.out
			memory(index) := summer.io.out
			when (maxReg < maxer.io.out) {
				maxReg := maxer.io.out
			}

			when (index === (memorySize - 1).U) {
				index := 0.U
				state := sSumming
			} .otherwise {
				index := index + 1.U
			}
		}
	}
}
