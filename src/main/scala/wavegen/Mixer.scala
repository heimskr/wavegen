package wavegen

import chisel3._
import chisel3.experimental._
import chisel3.util._
import scala.collection.immutable.ListMap

class Mixer(channelCount: Int, width: Int, memorySize: Int) extends Module {
	val sInit :: sSumming :: Nil = Enum(2)

	val io = IO(new Bundle {
		val in  = Vec(channelCount, Flipped(Decoupled(FixedPoint(width.W, 0.BP))))
		val out = Decoupled(FixedPoint(width.W, 0.BP))
		val state = Output(UInt(4.W))
	})

	val bigger = width + log2Ceil(channelCount)
	
	val state = RegInit(sInit)
	io.state := state
	
	io.out.valid := false.B
	io.out.bits := .0.F(0.BP)
	
	io.in.foreach { in => in.ready := true.B }
	
	val maxer = Module(new FPMaxer(channelCount + 1, FixedPoint(width.W, 0.BP)))
	val summer = Module(new FPSummer(channelCount, width))
	val maxReg = RegInit(summer.makeOut(.0))

	for (i <- 0 until channelCount) {
		maxer.io.in(i)  := io.in(i).bits
		summer.io.in(i) := io.in(i).bits
	}

	maxer.io.in(channelCount) := maxReg

	val allValid = io.in.foldLeft(true.B)(_ && _.valid)

	val memory = Reg(Vec(memorySize, summer.outType))
	val index = RegInit(0.U(log2Ceil(memorySize).W))

	0.F(0.BP)

	when (state === sInit) {
		when (allValid) {
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
