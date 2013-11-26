package shapelessTable {

import shapeless._

  class Table[TH, TR](val hdrs: TH, val rows: TR*)

  object Table {
    def apply[TH, TR](hdrs: TH, rows: TR*)
                     (implicit ts: TableShape[TH, TR]) = new Table(hdrs, rows: _*)

    trait TableShape[TH, TR]

    object TableShape {
      implicit def productTableShape[TH, TR, LH, LR]
      (implicit
       genH: Generic.Aux[TH, LH],
       genR: Generic.Aux[TR, LR],
       hlistShape: TableShape[LH, LR]): TableShape[TH, TR] = new TableShape[TH, TR] {}

      implicit def hsingleTableShape[RH]: TableShape[String :: HNil, RH :: HNil] =
        new TableShape[String :: HNil, RH :: HNil] {}

      implicit def hlistTableShape[HT <: HList, RH, RT <: HList]
      (implicit tailShape: TableShape[HT, RT]): TableShape[String :: HT, RH :: RT] =
        new TableShape[String :: HT, RH :: RT] {}
    }
  }
}

package shapeless {

  trait TableChecker {
    def forAll[TH, TR](table: shapelessTable.Table[TH, TR])(fun: TR => Unit) {
      table.rows.foreach { row =>
        fun(row)
      }
    }
  }

  object TableChecker extends TableChecker
}