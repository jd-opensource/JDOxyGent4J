import math
from decimal import Decimal, getcontext

from mcp.server.fastmcp import FastMCP
from pydantic import Field

mcp = FastMCP()


@mcp.tool(description="Power calculator tool")
def power(
    n: int = Field(description="base"),
    m: int = Field(description="index", default=2),
) -> int:
    return math.pow(n, m)


@mcp.tool(description="Pi tool")
def calc_pi(
    prec: int = Field(description="How many digits after the dot"),
) -> float:
    getcontext().prec = prec
    x = 0
    for k in range(int(prec / 8) + 1):
        a = 2 * Decimal.sqrt(Decimal(2)) / 9801
        b = math.factorial(4 * k) * (1103 + 26390 * k)
        c = pow(math.factorial(k), 4) * pow(396, 4 * k)
        x = x + a * b / c
    return 1 / x


if __name__ == "__main__":
    mcp.run()
