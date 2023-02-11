package wavegen

import chisel3._
import chisel3.util._
import scala.collection.immutable.ListMap

class Mixer(channelCount: Int, width: Int) extends Module {
	val init :: summing :: Nil = Enum(2)

	var inputs = Seq.tabulate(channelCount)(n => ("channel" + n) -> Flipped(Decoupled(UInt(width.W))))
	var channelKeys = inputs.map(_._1)
	val io = IO(new CustomBundle(inputs :+ ("out" -> Decoupled(UInt(width.W)))))
	val out = io("out").asInstanceOf[DecoupledIO[UInt]]

	def apply(channel: String) = io(channel).asInstanceOf[DecoupledIO[UInt]]

	val bigger = width + log2Ceil(channelCount)
	val maxReg = RegInit(0.U(bigger.W))

	val allValid = channelKeys.foldLeft(true.B)(_ && io(_).asInstanceOf[DecoupledIO[UInt]].valid)
	
	val state = RegInit(init)

	out.valid := false.B
	out.bits := 0.U

	channelKeys.foreach { this(_).ready := false.B }

	when (state === init) {
		out.valid := allValid
	}
}
