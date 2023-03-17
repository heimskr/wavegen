`timescale 1ns / 1ps

module FastPulseDomainCrosser (
    input  wire clock,
    input  wire reset, // active high
    input  wire slowClock,
    input  wire pulseIn,
    output wire pulseOut
);

    reg pulse;

    reg pulseCounter;

    always @(posedge slowClock) begin
        if (!reset) begin
            if (pulseCounter == 1'b0)
                pulse <= 1'b0;
            else
                pulseCounter <= pulseCounter - 1'b1;
        end
    end

    always @(posedge clock) begin
        if (reset) begin
            pulse <= 1'b0;
            pulseCounter <= 1'b1;
        end else if (pulseIn) begin
            pulse <= 1'b1;
            pulseCounter <= 1'b1;
        end
    end

    assign pulseOut = pulse;

endmodule
