use crate::base::error::MessageError;
use crate::with_message;
use std::fs;

const SINGLE_QUOTE: char = '\'';
const DOUBLE_QUOTE: char = '"';
const COMMENT_CHAR: char = '#';
const ESCAPE_CHAR: char = '\\';
const SPACE_CHAR: char = ' ';
const RETURN_CHAR: char = '\r';
const TABLE_CHAR: char = '\t';
const LF_CHAR: char = '\n';

// #[derive(PartialEq, Debug)]
// enum ParseState {
//     Default,
//     Item,
//     Comment,
//     Quote(char),
// }

pub(crate) fn parser(path: &str) -> Result<Vec<String>, MessageError> {
    let content = with_message!(fs::read_to_string(path), &format!("Failed to read vm cfg file: {}", path))?;
    // let mut state = ParseState::Default;
    let mut line_first = true;
    let mut items = Vec::new();
    let mut item = String::new();
    let mut not_escaped = true;
    let mut is_comment = false;
    let mut quote = SPACE_CHAR;

    let chars = content.chars();
    for c in chars {
        // ' ' \t \n \r  空白字符

        if is_comment {
            if c == LF_CHAR {
                line_first = true;
                is_comment = false;
            }
            continue;
        }

        match c {
            ESCAPE_CHAR if not_escaped => {
                not_escaped = false;
                line_first = false;
                continue;
            }
            COMMENT_CHAR if line_first => {
                is_comment = true;
                continue;
            }
            SINGLE_QUOTE | DOUBLE_QUOTE if not_escaped => {
                if quote != SPACE_CHAR {
                    quote = SPACE_CHAR;
                } else {
                    quote = c;
                }
            }
            SPACE_CHAR | RETURN_CHAR | TABLE_CHAR | LF_CHAR if quote == SPACE_CHAR => {
                if !item.is_empty() {
                    items.push(item.clone());
                    item.clear();
                }
                if c == LF_CHAR {
                    line_first = true;
                    not_escaped = true;
                    continue;
                }
            }
            c => {
                item.push(c);
            }
        }
        if !not_escaped {
            not_escaped = true;
        }
        if line_first {
            line_first = false;
        }
        //
        // match state {
        //     ParseState::Default => {
        //         match c {
        //             COMMENT_CHAR if line_first => {
        //                 state = ParseState::Comment;
        //                 continue;
        //             }
        //             SINGLE_QUOTE | DOUBLE_QUOTE => {
        //                 state = ParseState::Quote(c);
        //             }
        //             SPACE_CHAR | RETURN_CHAR | TABLE_CHAR | LF_CHAR => {
        //                 line_first = c == LF_CHAR;
        //                 continue;
        //             }
        //             c => {
        //                 state = ParseState::Item;
        //                 item.push(c);
        //             }
        //         }
        //     }
        //     ParseState::Item => {
        //         match c {
        //             SINGLE_QUOTE | DOUBLE_QUOTE => {
        //                 state = ParseState::Quote(c);
        //                 not_escaped = true;
        //             }
        //             SPACE_CHAR | RETURN_CHAR | TABLE_CHAR | LF_CHAR => {
        //                 items.push(item.clone());
        //                 item.clear();
        //                 state = ParseState::Default;
        //                 if c == LF_CHAR {
        //                     line_first = true;
        //                     continue;
        //                 }
        //             }
        //             c => {
        //                 item.push(c);
        //             }
        //         }
        //     }
        //     ParseState::Comment => {
        //         if c == LF_CHAR {
        //             state = ParseState::Default;
        //             line_first = true;
        //         }
        //         continue;
        //     }
        //     ParseState::Quote(q) => {
        //         match c {
        //             ESCAPE_CHAR if not_escaped => {
        //                 not_escaped = false;
        //             }
        //             c if c == q && not_escaped => {
        //                 state = ParseState::Item;
        //             }
        //             c => {
        //                 item.push(c);
        //                 not_escaped = true;
        //             }
        //         }
        //     }
        // }
        // if line_first {
        //     line_first = false;
        // }
    }
    Ok(items)
}


#[cfg(test)]
mod test {
    use crate::util::cfg_parser::parser;

    #[test]
    pub fn test_parser() {
        let r = parser("D:\\data\\code\\git\\own\\java-guard\\jg-launcher\\test\\test.cfg").unwrap();
        for x in r {
            println!("{:?}", x);
        }
    }
}