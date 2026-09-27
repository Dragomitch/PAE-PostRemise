# Country flags

One 24x24 PNG per ISO 3166-1 alpha-2 code (`<CODE>.png`), loaded by the front end as
`/images/flags/<CODE>.png`. The flag itself fills a 24x16 area (rows 4-19) with transparent
padding above and below. Every country seeded in `SQLRessources/init.sql` must have a
flag here; `CountryFlagsTest` enforces this.

## Credits

The following flags were added for issue #21:

| File     | Source                                                                                             |
|----------|----------------------------------------------------------------------------------------------------|
| `AQ.png` | Rendered from `flags/4x3/aq.svg` of [flag-icons](https://github.com/lipis/flag-icons) 7.5.0        |
| `EH.png` | Adapted from `flags/4x3/eh.svg` of flag-icons 7.5.0 (stripes snapped to the 24x16 pixel grid)      |
| `HM.png` | Copy of `AU.png` (Heard Island and McDonald Islands use the flag of Australia)                      |
| `SJ.png` | Copy of `NO.png` (Svalbard and Jan Mayen use the flag of Norway)                                    |
| `AN.png` | Drawn for this project (Netherlands Antilles, 1986-2010 flag)                                       |
| `CS.png` | Drawn for this project (Serbia and Montenegro, 1992-2006 flag, same palette as `RS.png`)            |

flag-icons is distributed under the MIT License:

> Copyright (c) 2013 Panayiotis Lipiridis
>
> Permission is hereby granted, free of charge, to any person obtaining a copy of
> this software and associated documentation files (the "Software"), to deal in
> the Software without restriction, including without limitation the rights to
> use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
> of the Software, and to permit persons to whom the Software is furnished to do
> so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.
