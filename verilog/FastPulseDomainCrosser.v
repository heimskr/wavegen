`timescale 1ns / 1ps

module FastPulseDomainCrosser (
    input  wire clock,
    input  wire reset, // active high
    input  wire slowClock,
    input  wire pulseIn,
    output wire pulseOut
);

    reg pulse;

    always @(negedge slowClock) begin
        pulse <= 1'b0;
    end

    always @(posedge clock) begin
        if (pulseIn)
            pulse <= 1'b1;
    end

    assign pulseOut = pulse;

endmodule
